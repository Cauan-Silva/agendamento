package com.cauan.agendamento.dto;

import com.cauan.agendamento.model.Bloqueio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class BloqueioDtos {

    private BloqueioDtos() {
    }

    /**
     * Exemplo (feriado, negócio inteiro):
     * { "profissionalId": null, "inicio": "2026-11-02T00:00", "fim": "2026-11-02T23:59", "motivo": "Finados" }
     */
    public record BloqueioRequest(
            Long profissionalId,
            @NotNull LocalDateTime inicio,
            @NotNull LocalDateTime fim,
            @Size(max = 100) String motivo) {
    }

    /** profissionalId e profissionalNome nulos significam "todo o negócio". */
    public record BloqueioResponse(Long id,
                                   Long profissionalId,
                                   String profissionalNome,
                                   LocalDateTime inicio,
                                   LocalDateTime fim,
                                   String motivo) {

        public static BloqueioResponse de(Bloqueio b) {
            boolean geral = b.getProfissional() == null;
            return new BloqueioResponse(
                    b.getId(),
                    geral ? null : b.getProfissional().getId(),
                    geral ? null : b.getProfissional().getNome(),
                    b.getInicio(),
                    b.getFim(),
                    b.getMotivo());
        }
    }
}
