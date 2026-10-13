package com.cauan.agendamento.notificacao;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.repository.AgendamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Todo dia (por padrão às 18h) envia lembrete para quem tem horário no dia seguinte.
 * Cada agendamento recebe no máximo um lembrete.
 */
@Service
public class LembreteService {

    private static final Logger log = LoggerFactory.getLogger(LembreteService.class);

    private final AgendamentoRepository repository;
    private final NotificacaoService notificacao;
    private final Clock clock;

    public LembreteService(AgendamentoRepository repository, NotificacaoService notificacao, Clock clock) {
        this.repository = repository;
        this.notificacao = notificacao;
        this.clock = clock;
    }

    @Scheduled(cron = "${agenda.lembrete.cron}", zone = "${agenda.fuso-horario}")
    @Transactional
    public void enviarLembretesDeAmanha() {
        LocalDate amanha = LocalDate.now(clock).plusDays(1);
        List<Agendamento> pendentes = repository.buscarParaLembrete(
                amanha.atStartOfDay(), amanha.atTime(23, 59, 59),
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO));

        int enviados = 0;
        for (Agendamento a : pendentes) {
            if (notificacao.enviarLembrete(AgendamentoResponse.de(a))) {
                a.setLembreteEnviado(true);
                enviados++;
            }
        }
        if (enviados > 0) {
            log.info("{} lembrete(s) enviado(s) para {}", enviados, amanha);
        }
    }
}
