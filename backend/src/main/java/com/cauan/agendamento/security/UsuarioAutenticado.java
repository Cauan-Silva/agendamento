package com.cauan.agendamento.security;

import com.cauan.agendamento.model.Perfil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

/**
 * Quem está logado no painel, montado a partir do token e do banco a cada requisição
 * (veja {@link ConversorJwt}). Os controllers usam para aplicar as regras do perfil.
 */
public class UsuarioAutenticado extends JwtAuthenticationToken {

    private final Long usuarioId;
    private final Perfil perfil;
    private final Long profissionalId;

    public UsuarioAutenticado(Jwt jwt, Collection<? extends GrantedAuthority> autoridades,
                              Long usuarioId, Perfil perfil, Long profissionalId) {
        super(jwt, autoridades, jwt.getSubject());
        this.usuarioId = usuarioId;
        this.perfil = perfil;
        this.profissionalId = profissionalId;
    }

    public static UsuarioAutenticado de(Authentication autenticacao) {
        if (autenticacao instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        throw new IllegalStateException("Requisição sem usuário do painel");
    }

    public Long usuarioId() { return usuarioId; }
    public Perfil perfil() { return perfil; }
    public Long profissionalId() { return profissionalId; }

    /**
     * Para filtrar a agenda: o profissional só enxerga a própria (devolve o id dele);
     * o administrador enxerga todos (devolve null).
     */
    public Long somenteDoProfissional() {
        return perfil == Perfil.PROFISSIONAL ? profissionalId : null;
    }
}
