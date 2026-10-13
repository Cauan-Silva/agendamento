package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoRequest;
import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;
import com.cauan.agendamento.exception.ConflitoHorarioException;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.notificacao.AgendamentoCriadoEvent;
import com.cauan.agendamento.repository.AgendamentoRepository;
import com.cauan.agendamento.repository.BloqueioRepository;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.ServicoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository repository,
                              ProfissionalRepository profissionalRepository,
                              ServicoRepository servicoRepository,
                              BloqueioRepository bloqueioRepository,
                              ApplicationEventPublisher eventos,
                              Clock clock) {
        this.repository = repository;
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest req) {
        // Trava o profissional: agendamentos simultâneos para ele entram em fila.
        Profissional profissional = profissionalRepository.findByIdParaAtualizar(req.profissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", req.profissionalId()));
        Servico servico = servicoRepository.findById(req.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", req.servicoId()));

        DisponibilidadeService.validarCombinacao(profissional, servico);

        LocalDateTime inicio = req.inicio().withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());

        if (!inicio.isAfter(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("Não é possível agendar em um horário que já passou");
        }
        if (!profissional.trabalhaEm(inicio.getDayOfWeek())) {
            throw new RegraNegocioException(profissional.getNome() + " não trabalha neste dia");
        }
        boolean foraDoExpediente = !fim.toLocalDate().equals(inicio.toLocalDate())
                || inicio.toLocalTime().isBefore(profissional.getHoraInicio())
                || fim.toLocalTime().isAfter(profissional.getHoraFim());
        if (foraDoExpediente) {
            throw new RegraNegocioException("Horário fora do expediente de " + profissional.getNome());
        }

        boolean bloqueado = profissional.almocoSobrepoe(inicio, fim)
                || !bloqueioRepository.buscarNoPeriodo(profissional.getId(), inicio, fim).isEmpty();
        if (bloqueado) {
            throw new RegraNegocioException("Este horário está bloqueado na agenda de " + profissional.getNome());
        }

        boolean ocupado = !repository.buscarOcupados(
                profissional.getId(), inicio, fim, StatusAgendamento.CANCELADO).isEmpty();
        if (ocupado) {
            throw new ConflitoHorarioException("Este horário acabou de ser reservado. Escolha outro.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setValor(servico.getPreco());
        agendamento.setClienteNome(req.clienteNome().trim());
        agendamento.setClienteTelefone(req.clienteTelefone());
        agendamento.setClienteEmail(normalizarEmail(req.clienteEmail()));
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setCriadoEm(LocalDateTime.now(clock));

        AgendamentoResponse resposta = AgendamentoResponse.de(repository.save(agendamento));
        // A confirmação por e-mail sai depois do commit (veja NotificacaoService).
        eventos.publishEvent(new AgendamentoCriadoEvent(resposta));
        return resposta;
    }

    private static String normalizarEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        return AgendamentoResponse.de(obter(id));
    }

    /** Agenda do dia; sem profissionalId, traz a equipe inteira. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarDoDia(Long profissionalId, LocalDate data) {
        LocalDateTime de = data.atStartOfDay();
        LocalDateTime ate = data.atTime(23, 59, 59);
        List<Agendamento> agendamentos = profissionalId == null
                ? repository.findByInicioBetweenOrderByInicio(de, ate)
                : repository.findByProfissionalIdAndInicioBetweenOrderByInicio(profissionalId, de, ate);
        return agendamentos.stream().map(AgendamentoResponse::de).toList();
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id) {
        Agendamento agendamento = obter(id);
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Um atendimento concluído não pode ser cancelado");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return AgendamentoResponse.de(agendamento);
    }

    @Transactional
    public AgendamentoResponse concluir(Long id) {
        Agendamento agendamento = obter(id);
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Um agendamento cancelado não pode ser concluído");
        }
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return AgendamentoResponse.de(agendamento);
    }

    private Agendamento obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }
}
