package com.cauan.agendamento.security;

import com.cauan.agendamento.model.Perfil;
import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;

/**
 * Transforma o token em um usuário do painel, consultando o banco a cada requisição.
 * Assim, desativar alguém ou mudar o perfil vale na hora, sem esperar o token expirar.
 */
public class ConversorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository repository;

    public ConversorJwt(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Object uid = jwt.getClaims().get("uid");
        if (!(uid instanceof Number numero)) {
            // Token antigo, de antes dos perfis: pede para entrar de novo.
            throw new InvalidBearerTokenException("Sessão antiga. Entre de novo.");
        }

        Usuario usuario = repository.buscarComProfissional(numero.longValue())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuário desativado ou removido"));

        Perfil perfil = usuario.getPerfil();
        Long profissionalId = usuario.getProfissional() == null ? null : usuario.getProfissional().getId();
        if (perfil == Perfil.PROFISSIONAL && profissionalId == null) {
            throw new InvalidBearerTokenException("Login sem profissional vinculado");
        }

        return new UsuarioAutenticado(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name())),
                usuario.getId(), perfil, profissionalId);
    }
}
