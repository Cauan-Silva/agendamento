package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoClienteResponse;
import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoRequest;
import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;
import com.cauan.agendamento.exception.ConflitoHorarioException;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.notificacao.AgendamentoAlteradoEvent;
import com.cauan.agendamento.notificacao.AgendamentoAlteradoEvent.Alteracao;
import com.cauan.agendamento.notificacao.AgendamentoCriadoEvent;
import com.cauan.agendamento.repository.AgendamentoRepository;
import com.cauan.agendamento.repository.BloqueioRepository;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AgendamentoService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final AgendamentoRepository repository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;
    private final long antecedenciaHoras;

    public AgendamentoService(AgendamentoRepository repository,
                              ProfissionalRepository profissionalRepository,
                              ServicoRepository servicoRepository,
                              BloqueioRepository bloqueioRepository,
                              ApplicationEventPublisher eventos,
                              Clock clock,
                              @Value("${agenda.cliente.antecedencia-horas}") long antecedenciaHoras) {
        this.repository = repository;
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.eventos = eventos;
        this.clock = clock;
        this.antecedenciaHoras = antecedenciaHoras;
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
        validarHorarioLivre(profissional, inicio, fim, null);

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
        agendamento.setCodigoAcesso(novoCodigoAcesso());

        AgendamentoResponse resposta = AgendamentoResponse.de(repository.save(agendamento));
        // A confirmação por e-mail sai depois do commit (veja NotificacaoService).
        eventos.publishEvent(new AgendamentoCriadoEvent(resposta));
        return resposta;
    }

    /**
     * Regras de horário comuns a criar e remarcar. {@code ignorarId} é o próprio
     * agendamento sendo remarcado, que não pode contar como conflito consigo mesmo.
     */
    private void validarHorarioLivre(Profissional profissional, LocalDateTime inicio, LocalDateTime fim,
                                     Long ignorarId) {
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

        boolean ocupado = repository.buscarOcupados(profissional.getId(), inicio, fim, StatusAgendamento.CANCELADO)
                .stream()
                .anyMatch(a -> ignorarId == null || !ignorarId.equals(a.getId()));
        if (ocupado) {
            throw new ConflitoHorarioException("Este horário acabou de ser reservado. Escolha outro.");
        }
    }

    private static String normalizarEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    /** 32 bytes aleatórios em Base64 para URL: impossível de adivinhar. */
    static String novoCodigoAcesso() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ---------- Equipe (painel) ----------

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        return buscar(id, null);
    }

    /** {@code somenteDoProfissional}: se informado, só enxerga agendamentos dessa pessoa. */
    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id, Long somenteDoProfissional) {
        return AgendamentoResponse.de(obter(id, somenteDoProfissional));
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
        return cancelar(id, null);
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id, Long somenteDoProfissional) {
        Agendamento agendamento = obter(id, somenteDoProfissional);
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Um atendimento concluído não pode ser cancelado");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return AgendamentoResponse.de(agendamento);
    }

    @Transactional
    public AgendamentoResponse concluir(Long id) {
        return concluir(id, null);
    }

    @Transactional
    public AgendamentoResponse concluir(Long id, Long somenteDoProfissional) {
        Agendamento agendamento = obter(id, somenteDoProfissional);
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Um agendamento cancelado não pode ser concluído");
        }
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return AgendamentoResponse.de(agendamento);
    }

    private Agendamento obter(Long id, Long somenteDoProfissional) {
        Agendamento agendamento = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        // Agendamento de outra pessoa da equipe: responde como se não existisse.
        if (somenteDoProfissional != null
                && !somenteDoProfissional.equals(agendamento.getProfissional().getId())) {
            throw new RecursoNaoEncontradoException("Agendamento", id);
        }
        return agendamento;
    }

    // ---------- Cliente (link do e-mail, sem login) ----------

    @Transactional(readOnly = true)
    public AgendamentoClienteResponse buscarPeloCodigo(String codigo) {
        return paraCliente(obterPeloCodigo(codigo));
    }

    @Transactional
    public AgendamentoClienteResponse cancelarPeloCliente(String codigo) {
        Agendamento agendamento = obterPeloCodigo(codigo);
        exigirQuePodeAlterar(agendamento, "cancelar");
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        eventos.publishEvent(new AgendamentoAlteradoEvent(AgendamentoResponse.de(agendamento), Alteracao.CANCELADO));
        return paraCliente(agendamento);
    }

    /** Mesmo profissional e mesmo serviço, em outro horário. */
    @Transactional
    public AgendamentoClienteResponse remarcarPeloCliente(String codigo, LocalDateTime novoInicio) {
        Agendamento agendamento = obterPeloCodigo(codigo);
        exigirQuePodeAlterar(agendamento, "remarcar");

        // Mesma trava da criação: evita dois clientes pegando o mesmo horário.
        Profissional profissional = profissionalRepository.findByIdParaAtualizar(agendamento.getProfissional().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", agendamento.getProfissional().getId()));
        Servico servico = agendamento.getServico();
        DisponibilidadeService.validarCombinacao(profissional, servico);

        LocalDateTime inicio = novoInicio.withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        validarHorarioLivre(profissional, inicio, fim, agendamento.getId());

        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setLembreteEnviado(false);
        eventos.publishEvent(new AgendamentoAlteradoEvent(AgendamentoResponse.de(agendamento), Alteracao.REMARCADO));
        return paraCliente(agendamento);
    }

    private Agendamento obterPeloCodigo(String codigo) {
        return repository.findByCodigoAcesso(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado. Confira o link."));
    }

    /** Até quando o cliente ainda pode mexer no horário pelo link. */
    private LocalDateTime limiteParaAlterar(Agendamento agendamento) {
        return agendamento.getInicio().minusHours(antecedenciaHoras);
    }

    private boolean podeAlterar(Agendamento agendamento) {
        boolean ativo = agendamento.getStatus() == StatusAgendamento.AGENDADO
                || agendamento.getStatus() == StatusAgendamento.CONFIRMADO;
        return ativo && LocalDateTime.now(clock).isBefore(limiteParaAlterar(agendamento));
    }

    private void exigirQuePodeAlterar(Agendamento agendamento, String acao) {
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Este agendamento já foi cancelado");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Este atendimento já foi realizado");
        }
        if (!podeAlterar(agendamento)) {
            throw new RegraNegocioException("Pelo link, só é possível " + acao + " até " + antecedenciaHoras
                    + " hora(s) antes do horário. Fale direto com o estabelecimento.");
        }
    }

    private AgendamentoClienteResponse paraCliente(Agendamento agendamento) {
        return AgendamentoClienteResponse.de(agendamento, podeAlterar(agendamento), limiteParaAlterar(agendamento));
    }
}
