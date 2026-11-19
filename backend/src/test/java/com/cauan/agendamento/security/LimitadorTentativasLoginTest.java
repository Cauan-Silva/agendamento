package com.cauan.agendamento.security;

import com.cauan.agendamento.exception.MuitasTentativasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorTentativasLoginTest {

    private static final String IP = "203.0.113.7";

    private RelogioAjustavel relogio;
    private LimitadorTentativasLogin limitador;

    @BeforeEach
    void setUp() {
        relogio = new RelogioAjustavel(Instant.parse("2026-10-13T12:00:00Z"));
        limitador = new LimitadorTentativasLogin(relogio);
    }

    private void errar(String login, String ip, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limitador.verificar(login, ip);
            limitador.registrarFalha(login, ip);
        }
    }

    @Test
    void quatroErrosAindaDeixamTentar() {
        errar("dono@barbearia.com", IP, 4);

        assertThatCode(() -> limitador.verificar("dono@barbearia.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void cincoErrosBloqueiamAteAJanelaPassar() {
        errar("dono@barbearia.com", IP, 5);

        assertThatThrownBy(() -> limitador.verificar("dono@barbearia.com", IP))
                .isInstanceOf(MuitasTentativasException.class)
                .hasMessageContaining("Muitas tentativas")
                .satisfies(e -> assertThat(((MuitasTentativasException) e).getEspera())
                        .isEqualTo(Duration.ofMinutes(15)));

        relogio.avancar(Duration.ofMinutes(14));
        assertThatThrownBy(() -> limitador.verificar("dono@barbearia.com", IP))
                .isInstanceOf(MuitasTentativasException.class)
                .satisfies(e -> assertThat(((MuitasTentativasException) e).getEspera())
                        .isEqualTo(Duration.ofMinutes(1)));

        relogio.avancar(Duration.ofMinutes(1));
        assertThatCode(() -> limitador.verificar("dono@barbearia.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void errosEspalhadosAlemDaJanelaNaoSeSomam() {
        errar("dono@barbearia.com", IP, 4);
        relogio.avancar(Duration.ofMinutes(16));
        errar("dono@barbearia.com", IP, 4);

        assertThatCode(() -> limitador.verificar("dono@barbearia.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void loginCertoZeraAsFalhas() {
        errar("dono@barbearia.com", IP, 4);
        limitador.registrarSucesso("dono@barbearia.com", IP);
        errar("dono@barbearia.com", IP, 4);

        assertThatCode(() -> limitador.verificar("dono@barbearia.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void contaPorLoginEIpIgnorandoMaiusculas() {
        errar("Dono@Barbearia.com ", IP, 5);

        assertThatThrownBy(() -> limitador.verificar("dono@barbearia.com", IP))
                .isInstanceOf(MuitasTentativasException.class);
        assertThatCode(() -> limitador.verificar("dono@barbearia.com", "198.51.100.1")).doesNotThrowAnyException();
        assertThatCode(() -> limitador.verificar("ana@barbearia.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void memoriaFicaLimitadaERegistrosVencidosSaem() {
        for (int i = 0; i < LimitadorTentativasLogin.MAX_REGISTROS; i++) {
            limitador.registrarFalha("pessoa" + i + "@teste.com", IP);
        }
        assertThat(limitador.tamanho()).isEqualTo(LimitadorTentativasLogin.MAX_REGISTROS);

        // Mesmo sob uma enxurrada de logins diferentes, não passa do limite.
        limitador.registrarFalha("mais-um@teste.com", IP);
        assertThat(limitador.tamanho()).isLessThanOrEqualTo(LimitadorTentativasLogin.MAX_REGISTROS);

        // Depois da janela, os registros vencidos são descartados.
        relogio.avancar(Duration.ofMinutes(16));
        limitador.registrarFalha("novo@teste.com", IP);
        assertThat(limitador.tamanho()).isEqualTo(1);
    }
}
