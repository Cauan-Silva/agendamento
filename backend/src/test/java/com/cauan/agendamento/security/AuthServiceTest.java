package com.cauan.agendamento.security;

import com.cauan.agendamento.dto.AuthDtos.LoginRequest;
import com.cauan.agendamento.dto.AuthDtos.LoginResponse;
import com.cauan.agendamento.exception.CredenciaisInvalidasException;
import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-13T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtEncoder jwtEncoder;

    private AuthService service;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        service = new AuthService(usuarioRepository, passwordEncoder, jwtEncoder,
                Clock.fixed(AGORA, ZoneOffset.UTC), 12);
        dono = new Usuario("Dono", "dono@barbearia.com", "hash");
    }

    @Test
    void loginCorretoDevolveTokenComValidade() {
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        when(jwtEncoder.encode(any())).thenReturn(
                Jwt.withTokenValue("token-teste").header("alg", "HS256").claim("sub", "dono@barbearia.com").build());

        LoginResponse resposta = service.entrar(new LoginRequest("dono@barbearia.com", "certa"));

        assertThat(resposta.token()).isEqualTo("token-teste");
        assertThat(resposta.nome()).isEqualTo("Dono");
        assertThat(resposta.expiraEm()).isEqualTo(AGORA.plusSeconds(12 * 3600));
    }

    @Test
    void senhaErradaNaoGeraToken() {
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.entrar(new LoginRequest("dono@barbearia.com", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void usuarioDesativadoNaoEntra() {
        dono.setAtivo(false);
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));

        assertThatThrownBy(() -> service.entrar(new LoginRequest("dono@barbearia.com", "certa")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }
}
