package com.cauan.agendamento.security;

import com.cauan.agendamento.dto.AuthDtos.LoginRequest;
import com.cauan.agendamento.dto.AuthDtos.LoginResponse;
import com.cauan.agendamento.exception.CredenciaisInvalidasException;
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
    private final Duration validade;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       Clock clock,
                       @Value("${agenda.jwt.validade-horas}") long validadeHoras) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.validade = Duration.ofHours(validadeHoras);
    }

    @Transactional(readOnly = true)
    public LoginResponse entrar(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);

        Instant agora = clock.instant();
        Instant expira = agora.plus(validade);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("agendamento-api")
                .issuedAt(agora)
                .expiresAt(expira)
                .subject(usuario.getEmail())
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new LoginResponse(token, usuario.getNome(), expira);
    }
}
