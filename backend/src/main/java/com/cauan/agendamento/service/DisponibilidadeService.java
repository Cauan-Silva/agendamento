package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.AgendamentoDtos.DisponibilidadeResponse;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.repository.AgendamentoRepository;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula os horários livres de um profissional para um serviço em um dia.
 *
 * Passo a passo:
 * 1. Se o profissional não trabalha nesse dia da semana, não há horários.
 * 2. Percorre o expediente em passos de {@code agenda.intervalo-minutos}
 *    (ex.: 09:00, 09:30, 10:00...), considerando a duração do serviço.
 * 3. Descarta horários que já passaram.
 * 4. Descarta horários que se sobrepõem a agendamentos não cancelados.
 */
@Service
public class DisponibilidadeService {

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final Clock clock;
    private final int intervaloMinutos;

    public DisponibilidadeService(ProfissionalRepository profissionalRepository,
                                  ServicoRepository servicoRepository,
                                  AgendamentoRepository agendamentoRepository,
                                  Clock clock,
                                  @Value("${agenda.intervalo-minutos}") int intervaloMinutos) {
        if (intervaloMinutos <= 0) {
            throw new IllegalArgumentException("agenda.intervalo-minutos deve ser maior que zero");
        }
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.clock = clock;
        this.intervaloMinutos = intervaloMinutos;
    }

    @Transactional(readOnly = true)
    public DisponibilidadeResponse consultar(Long profissionalId, Long servicoId, LocalDate data) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", profissionalId));
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", servicoId));

        validarCombinacao(profissional, servico);

        return new DisponibilidadeResponse(profissionalId, servicoId, data,
                calcularHorarios(profissional, servico, data));
    }

    List<LocalTime> calcularHorarios(Profissional profissional, Servico servico, LocalDate data) {
        if (!profissional.trabalhaEm(data.getDayOfWeek())) {
            return List.of();
        }

        LocalDateTime inicioExpediente = data.atTime(profissional.getHoraInicio());
        LocalDateTime fimExpediente = data.atTime(profissional.getHoraFim());
        LocalDateTime agora = LocalDateTime.now(clock);

        List<Agendamento> ocupados = agendamentoRepository.buscarOcupados(
                profissional.getId(), inicioExpediente, fimExpediente, StatusAgendamento.CANCELADO);

        List<LocalTime> livres = new ArrayList<>();
        for (LocalDateTime inicio = inicioExpediente;
             !inicio.plusMinutes(servico.getDuracaoMinutos()).isAfter(fimExpediente);
             inicio = inicio.plusMinutes(intervaloMinutos)) {

            LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
            if (!inicio.isAfter(agora)) {
                continue;
            }

            LocalDateTime slotInicio = inicio;
            boolean ocupado = ocupados.stream().anyMatch(a -> a.sobrepoe(slotInicio, fim));
            if (!ocupado) {
                livres.add(inicio.toLocalTime());
            }
        }
        return livres;
    }

    /** Regras comuns à consulta de horários e à criação de agendamentos. */
    static void validarCombinacao(Profissional profissional, Servico servico) {
        if (!profissional.isAtivo()) {
            throw new RegraNegocioException("Profissional inativo");
        }
        if (!servico.isAtivo()) {
            throw new RegraNegocioException("Serviço inativo");
        }
        if (!profissional.atende(servico.getId())) {
            throw new RegraNegocioException(
                    profissional.getNome() + " não realiza o serviço " + servico.getNome());
        }
    }
}
