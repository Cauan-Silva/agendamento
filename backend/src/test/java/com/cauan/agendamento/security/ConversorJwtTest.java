package com.cauan.agendamento.security;

import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversorJwtTest {

    @Mock
    private UsuarioRepository repository;

    private ConversorJwt conversor;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        conversor = new ConversorJwt(repository);
        dono = new Usuario("Dono", "dono@barbearia.com", "hash");
        dono.setId(1L);
        when(repository.buscarComProfissional(1L)).thenReturn(Optional.of(dono));
    }

    private static Jwt token(Integer versao) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "HS256")
                .subject("dono@barbearia.com").claim("uid", 1L);
        if (versao != null) {
            builder.claim("ver", versao);
        }
        return builder.build();
    }

    @Test
    void tokenDaVersaoAtualVale() {
        UsuarioAutenticado autenticado = (UsuarioAutenticado) conversor.convert(token(0));

        assertThat(autenticado.usuarioId()).isEqualTo(1L);
    }

    @Test
    void tokenEmitidoAntesDaTrocaDeSenhaDeixaDeValer() {
        dono.invalidarTokens(); // senha trocada depois que o token foi emitido

        assertThatThrownBy(() -> conversor.convert(token(0)))
                .isInstanceOf(InvalidBearerTokenException.class)
                .hasMessageContaining("senha");
        assertThat(conversor.convert(token(1))).isNotNull();
    }

    @Test
    void tokenSemVersaoValeSoEnquantoASenhaNaoMudar() {
        assertThat(conversor.convert(token(null))).isNotNull();

        dono.invalidarTokens();
        assertThatThrownBy(() -> conversor.convert(token(null)))
                .isInstanceOf(InvalidBearerTokenException.class);
    }
}
