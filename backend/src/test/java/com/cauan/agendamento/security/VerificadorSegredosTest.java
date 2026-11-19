package com.cauan.agendamento.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificadorSegredosTest {

    private static final String SEGREDO_FORTE = "um-segredo-bem-aleatorio-com-mais-de-32-bytes-9f8e7d";

    @Test
    void producaoComSegredoPadraoNaoSobe() {
        assertThatThrownBy(() -> VerificadorSegredos.verificar(
                VerificadorSegredos.SEGREDO_JWT_PADRAO, "senha-forte-do-dono", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SEGREDO");
    }

    @Test
    void producaoComSenhaPadraoDoAdminNaoSobe() {
        assertThatThrownBy(() -> VerificadorSegredos.verificar(
                SEGREDO_FORTE, VerificadorSegredos.SENHA_ADMIN_PADRAO, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_SENHA");
    }

    @Test
    void segredoCurtoNaoSobeNemEmDesenvolvimento() {
        assertThatThrownBy(() -> VerificadorSegredos.verificar("curto-demais", "senha-forte-do-dono", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void desenvolvimentoAceitaOsValoresPadrao() {
        assertThatCode(() -> VerificadorSegredos.verificar(
                VerificadorSegredos.SEGREDO_JWT_PADRAO, VerificadorSegredos.SENHA_ADMIN_PADRAO, true))
                .doesNotThrowAnyException();
    }

    @Test
    void producaoComValoresProprios() {
        assertThatCode(() -> VerificadorSegredos.verificar(SEGREDO_FORTE, "senha-forte-do-dono", false))
                .doesNotThrowAnyException();
    }
}
