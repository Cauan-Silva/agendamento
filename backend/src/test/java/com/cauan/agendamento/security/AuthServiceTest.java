package com.cauan.agendamento.security;

import com.cauan.agendamento.dto.AuthDtos.LoginRequest;
import com.cauan.agendamento.dto.AuthDtos.LoginResponse;
import com.cauan.agendamento.exception.CredenciaisInvalidasException;
import com.cauan.agendamento.exception.MuitasTentativasException;
import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-13T12:00:00Z");
    private static final String IP = "203.0.113.7";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtEncoder jwtEncoder;

    private AuthService service;
    private LimitadorTentativasLogin limitador;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        RelogioAjustavel relogio = new RelogioAjustavel(AGORA);
        limitador = new LimitadorTentativasLogin(relogio);
        service = new AuthService(usuarioRepository, passwordEncoder, jwtEncoder, relogio, limitador, 12);
        dono = new Usuario("Dono", "dono@barbearia.com", "hash");
        dono.setId(1L);
    }

    private void tokenDeTeste() {
        when(jwtEncoder.encode(any())).thenReturn(
                Jwt.withTokenValue("token-teste").header("alg", "HS256").claim("sub", "dono@barbearia.com").build());
    }

    private void errarSenha(int vezes) {
        for (int i = 0; i < vezes; i++) {
            assertThatThrownBy(() -> service.entrar(new LoginRequest("dono@barbearia.com", "errada"), IP))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }
    }

    @Test
    void loginCorretoDevolveTokenComValidade() {
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        tokenDeTeste();

        LoginResponse resposta = service.entrar(new LoginRequest("dono@barbearia.com", "certa"), IP);

        assertThat(resposta.token()).isEqualTo("token-teste");
        assertThat(resposta.nome()).isEqualTo("Dono");
        assertThat(resposta.expiraEm()).isEqualTo(AGORA.plusSeconds(12 * 3600));
    }

    @Test
    void tokenLevaAVersaoAtualDoLogin() {
        dono.invalidarTokens();
        dono.invalidarTokens();
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        tokenDeTeste();

        service.entrar(new LoginRequest("dono@barbearia.com", "certa"), IP);

        ArgumentCaptor<JwtEncoderParameters> parametros = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parametros.capture());
        assertThat(parametros.getValue().getClaims().<Object>getClaim("ver")).isEqualTo(2);
    }

    @Test
    void senhaErradaNaoGeraToken() {
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.entrar(new LoginRequest("dono@barbearia.com", "errada"), IP))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void usuarioDesativadoNaoEntra() {
        dono.setAtivo(false);
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));

        assertThatThrownBy(() -> service.entrar(new LoginRequest("dono@barbearia.com", "certa"), IP))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void cincoSenhasErradasBloqueiamMesmoASenhaCerta() {
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
        errarSenha(5);

        assertThatThrownBy(() -> service.entrar(new LoginRequest("dono@barbearia.com", "certa"), IP))
                .isInstanceOf(MuitasTentativasException.class);
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void loginInexistenteTambemContaComoFalha() {
        when(usuarioRepository.findByEmailIgnoreCase("ninguem@barbearia.com")).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.entrar(new LoginRequest("ninguem@barbearia.com", "x"), IP))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }

        assertThatThrownBy(() -> service.entrar(new LoginRequest("ninguem@barbearia.com", "x"), IP))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void loginCertoZeraAsFalhas() {
        when(usuarioRepository.findByEmailIgnoreCase("dono@barbearia.com")).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
        when(passwordEncoder.matches("certa", "hash")).thenReturn(true);
        tokenDeTeste();

        errarSenha(4);
        service.entrar(new LoginRequest("dono@barbearia.com", "certa"), IP);
        errarSenha(4);

        assertThatCode(() -> limitador.verificar("dono@barbearia.com", IP)).doesNotThrowAnyException();
    }
}
