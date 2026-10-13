package com.cauan.agendamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RelatorioDtos {

    private RelatorioDtos() {
    }

    /**
     * Resumo de um mês.
     * - atendimentos: agendamentos não cancelados
     * - faturamento: soma dos concluídos (dinheiro que de fato entrou)
     * - previsto: soma de tudo que não foi cancelado
     */
    public record RelatorioMensal(int ano,
                                  int mes,
                                  int atendimentos,
                                  int concluidos,
                                  int cancelados,
                                  BigDecimal faturamento,
                                  BigDecimal previsto,
                                  List<PorProfissional> porProfissional,
                                  List<PorServico> porServico,
                                  List<PorDia> porDia) {
    }

    public record PorProfissional(String nome, int atendimentos, int concluidos, BigDecimal faturamento) {
    }

    public record PorServico(String nome, int quantidade, BigDecimal faturamento) {
    }

    public record PorDia(LocalDate data, int atendimentos) {
    }
}
