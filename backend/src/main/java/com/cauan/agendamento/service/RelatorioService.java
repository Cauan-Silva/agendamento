package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.RelatorioDtos.PorDia;
import com.cauan.agendamento.dto.RelatorioDtos.PorProfissional;
import com.cauan.agendamento.dto.RelatorioDtos.PorServico;
import com.cauan.agendamento.dto.RelatorioDtos.RelatorioMensal;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.repository.AgendamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private final AgendamentoRepository repository;

    public RelatorioService(AgendamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RelatorioMensal mensal(YearMonth mes) {
        List<Agendamento> todos = repository.findByInicioBetweenOrderByInicio(
                mes.atDay(1).atStartOfDay(), mes.atEndOfMonth().atTime(23, 59, 59));

        int cancelados = 0;
        int concluidos = 0;
        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal previsto = BigDecimal.ZERO;

        Map<String, Acumulado> profissionais = new LinkedHashMap<>();
        Map<String, Acumulado> servicos = new LinkedHashMap<>();
        Map<LocalDate, Integer> dias = new LinkedHashMap<>();
        for (int d = 1; d <= mes.lengthOfMonth(); d++) {
            dias.put(mes.atDay(d), 0);
        }

        for (Agendamento a : todos) {
            if (a.getStatus() == StatusAgendamento.CANCELADO) {
                cancelados++;
                continue;
            }
            boolean concluido = a.getStatus() == StatusAgendamento.CONCLUIDO;
            previsto = previsto.add(a.getValor());
            if (concluido) {
                concluidos++;
                faturamento = faturamento.add(a.getValor());
            }

            profissionais.computeIfAbsent(a.getProfissional().getNome(), n -> new Acumulado()).somar(a, concluido);
            servicos.computeIfAbsent(a.getServico().getNome(), n -> new Acumulado()).somar(a, concluido);
            dias.merge(a.getInicio().toLocalDate(), 1, Integer::sum);
        }

        List<PorProfissional> porProfissional = new ArrayList<>();
        profissionais.forEach((nome, ac) ->
                porProfissional.add(new PorProfissional(nome, ac.quantidade, ac.concluidos, ac.faturamento)));
        porProfissional.sort(Comparator.comparing(PorProfissional::faturamento).reversed()
                .thenComparing(PorProfissional::nome));

        List<PorServico> porServico = new ArrayList<>();
        servicos.forEach((nome, ac) -> porServico.add(new PorServico(nome, ac.quantidade, ac.faturamento)));
        porServico.sort(Comparator.comparingInt(PorServico::quantidade).reversed()
                .thenComparing(PorServico::nome));

        List<PorDia> porDia = dias.entrySet().stream()
                .map(e -> new PorDia(e.getKey(), e.getValue()))
                .toList();

        return new RelatorioMensal(mes.getYear(), mes.getMonthValue(),
                todos.size() - cancelados, concluidos, cancelados,
                faturamento, previsto, porProfissional, porServico, porDia);
    }

    /** Somas por profissional ou serviço. Faturamento conta só os concluídos. */
    private static final class Acumulado {
        int quantidade;
        int concluidos;
        BigDecimal faturamento = BigDecimal.ZERO;

        void somar(Agendamento a, boolean concluido) {
            quantidade++;
            if (concluido) {
                concluidos++;
                faturamento = faturamento.add(a.getValor());
            }
        }
    }
}
