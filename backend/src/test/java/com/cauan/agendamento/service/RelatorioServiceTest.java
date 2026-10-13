package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.RelatorioDtos.RelatorioMensal;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.repository.AgendamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private AgendamentoRepository repository;

    private static Profissional profissional(String nome) {
        Profissional p = new Profissional();
        p.setNome(nome);
        return p;
    }

    private static Agendamento agendamento(Profissional p, String servico, String valor,
                                           LocalDateTime inicio, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setProfissional(p);
        a.setServico(new Servico(servico, 30, new BigDecimal(valor)));
        a.setValor(new BigDecimal(valor));
        a.setInicio(inicio);
        a.setFim(inicio.plusMinutes(30));
        a.setStatus(status);
        return a;
    }

    @Test
    void somaFaturamentoSoDosConcluidosEIgnoraCancelados() {
        Profissional ana = profissional("Ana");
        Profissional bruno = profissional("Bruno");
        LocalDate dia5 = LocalDate.of(2026, 10, 5);
        when(repository.findByInicioBetweenOrderByInicio(any(), any())).thenReturn(List.of(
                agendamento(ana, "Corte", "45.00", dia5.atTime(9, 0), StatusAgendamento.CONCLUIDO),
                agendamento(ana, "Barba", "35.00", dia5.atTime(10, 0), StatusAgendamento.CONCLUIDO),
                agendamento(bruno, "Corte", "45.00", dia5.atTime(14, 0), StatusAgendamento.AGENDADO),
                agendamento(bruno, "Corte", "45.00", dia5.atTime(15, 0), StatusAgendamento.CANCELADO)));

        RelatorioMensal r = new RelatorioService(repository).mensal(YearMonth.of(2026, 10));

        assertThat(r.atendimentos()).isEqualTo(3);
        assertThat(r.concluidos()).isEqualTo(2);
        assertThat(r.cancelados()).isEqualTo(1);
        assertThat(r.faturamento()).isEqualByComparingTo("80.00");
        assertThat(r.previsto()).isEqualByComparingTo("125.00");

        // Ana faturou mais, então vem primeiro
        assertThat(r.porProfissional().stream().map(p -> p.nome()).toList()).containsExactly("Ana", "Bruno");
        assertThat(r.porProfissional().get(1).faturamento()).isEqualByComparingTo("0");

        // Corte foi o mais feito (2, sem contar o cancelado)
        assertThat(r.porServico().get(0).nome()).isEqualTo("Corte");
        assertThat(r.porServico().get(0).quantidade()).isEqualTo(2);

        // Um item por dia do mês, com 3 atendimentos no dia 5
        assertThat(r.porDia()).hasSize(31);
        assertThat(r.porDia().get(4).atendimentos()).isEqualTo(3);
    }

    @Test
    void mesSemAgendamentosVemZerado() {
        when(repository.findByInicioBetweenOrderByInicio(any(), any())).thenReturn(List.of());

        RelatorioMensal r = new RelatorioService(repository).mensal(YearMonth.of(2026, 2));

        assertThat(r.atendimentos()).isZero();
        assertThat(r.faturamento()).isEqualByComparingTo("0");
        assertThat(r.porDia()).hasSize(28);
    }
}
