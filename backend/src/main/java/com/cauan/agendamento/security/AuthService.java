package com.cauan.agendamento.security;

import com.cauan.agendamento.dto.AuthDtos.LoginRequest;
import com.cauan.agendamento.dto.AuthDtos.LoginResponse;
import com.cauan.agendamento.exception.CredenciaisInvalidasException;
import com.cauan.agendamento.exception.MuitasTentativasException;
import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final LimitadorTentativasLogin limitador;
    private final Duration validade;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       Clock clock,
                       LimitadorTentativasLogin limitador,
                       @Value("${agenda.jwt.validade-horas}") long validadeHoras) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.limitador = limitador;
        this.validade = Duration.ofHours(validadeHoras);
    }

    /**
     * @param ip endereço de quem está tentando entrar: as senhas erradas são contadas por login + IP
     * @throws MuitasTentativasException depois de muitas senhas erradas seguidas (veja {@link LimitadorTentativasLogin})
     */
    @Transactional(readOnly = true)
    public LoginResponse entrar(LoginRequest req, String ip) {
        String email = req.email().trim();
        limitador.verificar(email, ip);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElse(null);
        if (usuario == null) {
            limitador.registrarFalha(email, ip);
            throw new CredenciaisInvalidasException();
        }
        limitador.registrarSucesso(email, ip);

        Instant agora = clock.instant();
        Instant expira = agora.plus(validade);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("agendamento-api")
                .issuedAt(agora)
                .expiresAt(expira)
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("ver", usuario.getVersaoToken())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        Long profissionalId = usuario.getProfissional() == null ? null : usuario.getProfissional().getId();
        String profissionalNome = usuario.getProfissional() == null ? null : usuario.getProfissional().getNome();
        return new LoginResponse(token, usuario.getId(), usuario.getNome(), expira, usuario.getPerfil(), profissionalId,
                profissionalNome);
    }
}
