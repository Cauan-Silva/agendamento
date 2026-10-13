package com.cauan.agendamento.dto;

import com.cauan.agendamento.model.Servico;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ServicoDtos {

    private ServicoDtos() {
    }

    public record ServicoRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotNull @Min(5) @Max(480) Integer duracaoMinutos,
            @NotNull @DecimalMin("0.00") BigDecimal preco) {
    }

    public record ServicoResponse(Long id, String nome, Integer duracaoMinutos, BigDecimal preco, boolean ativo) {

        public static ServicoResponse de(Servico s) {
            return new ServicoResponse(s.getId(), s.getNome(), s.getDuracaoMinutos(), s.getPreco(), s.isAtivo());
        }
    }
}
