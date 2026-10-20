package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoRequest;
import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;
import com.cauan.agendamento.exception.ConflitoHorarioException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.Bloqueio;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoClienteResponse;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.notificacao.AgendamentoAlteradoEvent;
import com.cauan.agendamento.notificacao.AgendamentoCriadoEvent;
import com.cauan.agendamento.repository.AgendamentoRepository;
import com.cauan.agendamento.repository.BloqueioRepository;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.ServicoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 19);

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ProfissionalRepository profissionalRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private BloqueioRepository bloqueioRepository;
    @Mock
    private ApplicationEventPublisher eventos;

    private AgendamentoService service;
    private Profissional ana;
    private Servico corte;

    @BeforeEach
    void setUp() {
        Clock agora = Clock.fixed(LocalDateTime.of(2026, 10, 12, 8, 0).atZone(FUSO).toInstant(), FUSO);
        service = new AgendamentoService(agendamentoRepository, profissionalRepository, servicoRepository,
                bloqueioRepository, eventos, agora, 2);

        corte = new Servico("Corte de cabelo", 30, new BigDecimal("45.00"));
        corte.setId(1L);

        ana = new Profissional();
        ana.setId(1L);
        ana.setNome("Ana");
        ana.setHoraInicio(LocalTime.of(9, 0));
        ana.setHoraFim(LocalTime.of(18, 0));
        ana.setDiasTrabalho(Set.of(DayOfWeek.MONDAY));
        ana.setServicos(Set.of(corte));
    }

    private AgendamentoRequest pedido(LocalDateTime inicio) {
        return new AgendamentoRequest(1L, 1L, inicio, "João", "48999990000", " Joao@Email.com ");
    }

    private void profissionalEServicoExistem() {
        when(profissionalRepository.findByIdParaAtualizar(1L)).thenReturn(Optional.of(ana));
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(corte));
    }

    @Test
    void criaAgendamentoCalculandoOFimPelaDuracaoDoServico() {
        profissionalEServicoExistem();
        when(agendamentoRepository.buscarOcupados(any(), any(), any(), any())).thenReturn(List.of());
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        AgendamentoResponse resposta = service.criar(pedido(SEGUNDA.atTime(14, 0)));

        assertThat(resposta.inicio()).isEqualTo(SEGUNDA.atTime(14, 0));
        assertThat(resposta.fim()).isEqualTo(SEGUNDA.atTime(14, 30));
        assertThat(resposta.status()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(resposta.valor()).isEqualByComparingTo("45.00");
        assertThat(resposta.clienteEmail()).isEqualTo("joao@email.com");
        verify(eventos).publishEvent(any(AgendamentoCriadoEvent.class));
    }

    @Test
    void recusaHorarioNoAlmoco() {
        profissionalEServicoExistem();
        ana.setAlmocoInicio(LocalTime.of(12, 0));
        ana.setAlmocoFim(LocalTime.of(13, 0));

        assertThatThrownBy(() -> service.criar(pedido(SEGUNDA.atTime(12, 15))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("bloqueado");
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void recusaHorarioComBloqueio() {
        profissionalEServicoExistem();
        Bloqueio folga = new Bloqueio();
        folga.setInicio(SEGUNDA.atTime(14, 0));
        folga.setFim(SEGUNDA.atTime(18, 0));
        when(bloqueioRepository.buscarNoPeriodo(any(), any(), any())).thenReturn(List.of(folga));

        assertThatThrownBy(() -> service.criar(pedido(SEGUNDA.atTime(15, 0))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("bloqueado");
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void recusaHorarioJaOcupado() {
        profissionalEServicoExistem();
        Agendamento existente = new Agendamento();
        existente.setInicio(SEGUNDA.atTime(14, 0));
        existente.setFim(SEGUNDA.atTime(14, 30));
        when(agendamentoRepository.buscarOcupados(any(), any(), any(), any())).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(pedido(SEGUNDA.atTime(14, 0))))
                .isInstanceOf(ConflitoHorarioException.class);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void recusaHorarioQueTerminaDepoisDoExpediente() {
        profissionalEServicoExistem();

        assertThatThrownBy(() -> service.criar(pedido(SEGUNDA.atTime(17, 45))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fora do expediente");
    }

    @Test
    void recusaDiaEmQueOProfissionalNaoTrabalha() {
        profissionalEServicoExistem();

        assertThatThrownBy(() -> service.criar(pedido(SEGUNDA.plusDays(1).atTime(10, 0))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não trabalha");
    }

    @Test
    void naoCancelaAtendimentoConcluido() {
        Agendamento concluido = new Agendamento();
        concluido.setStatus(StatusAgendamento.CONCLUIDO);
        when(agendamentoRepository.findById(5L)).thenReturn(Optional.of(concluido));

        assertThatThrownBy(() -> service.cancelar(5L)).isInstanceOf(RegraNegocioException.class);
    }

    // ---------- Cliente pelo link do e-mail ----------

    /** Agendamento da Ana na segunda às 14h; "agora" é a segunda anterior às 8h. */
    private Agendamento agendamentoDoCliente() {
        Agendamento a = new Agendamento();
        a.setId(7L);
        a.setProfissional(ana);
        a.setServico(corte);
        a.setValor(corte.getPreco());
        a.setClienteNome("João");
        a.setClienteTelefone("48999990000");
        a.setInicio(SEGUNDA.atTime(14, 0));
        a.setFim(SEGUNDA.atTime(14, 30));
        a.setStatus(StatusAgendamento.AGENDADO);
        a.setCodigoAcesso("codigo-secreto");
        return a;
    }

    @Test
    void criaAgendamentoComCodigoDeAcessoUnico() {
        profissionalEServicoExistem();
        when(agendamentoRepository.buscarOcupados(any(), any(), any(), any())).thenReturn(List.of());
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        String primeiro = service.criar(pedido(SEGUNDA.atTime(14, 0))).codigoAcesso();
        String segundo = service.criar(pedido(SEGUNDA.atTime(15, 0))).codigoAcesso();

        assertThat(primeiro).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(segundo).isNotEqualTo(primeiro);
    }

    @Test
    void clienteConsultaPeloCodigoESabeAteQuandoPodeAlterar() {
        when(agendamentoRepository.findByCodigoAcesso("codigo-secreto")).thenReturn(Optional.of(agendamentoDoCliente()));

        AgendamentoClienteResponse resposta = service.buscarPeloCodigo("codigo-secreto");

        assertThat(resposta.podeAlterar()).isTrue();
        assertThat(resposta.alterarAte()).isEqualTo(SEGUNDA.atTime(12, 0));
    }

    @Test
    void codigoInexistenteDa404() {
        when(agendamentoRepository.findByCodigoAcesso("errado")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPeloCodigo("errado"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void clienteCancelaPeloLinkEAvisaPorEmail() {
        Agendamento agendamento = agendamentoDoCliente();
        when(agendamentoRepository.findByCodigoAcesso("codigo-secreto")).thenReturn(Optional.of(agendamento));

        AgendamentoClienteResponse resposta = service.cancelarPeloCliente("codigo-secreto");

        assertThat(resposta.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(resposta.podeAlterar()).isFalse();
        verify(eventos).publishEvent(any(AgendamentoAlteradoEvent.class));
    }

    @Test
    void clienteNaoCancelaEmCimaDaHora() {
        Agendamento agendamento = agendamentoDoCliente();
        agendamento.setInicio(LocalDateTime.of(2026, 10, 12, 9, 0)); // 1h depois de "agora"
        agendamento.setFim(LocalDateTime.of(2026, 10, 12, 9, 30));
        when(agendamentoRepository.findByCodigoAcesso("codigo-secreto")).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.cancelarPeloCliente("codigo-secreto"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("2 hora(s) antes");
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    void clienteNaoMexeEmAgendamentoJaCancelado() {
        Agendamento agendamento = agendamentoDoCliente();
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        when(agendamentoRepository.findByCodigoAcesso("codigo-secreto")).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.remarcarPeloCliente("codigo-secreto", SEGUNDA.atTime(16, 0)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi cancelado");
    }

    @Test
    void clienteRemarcaParaOutroHorarioLivre() {
        Agendamento agendamento = agendamentoDoCliente();
        agendamento.setLembreteEnviado(true);
        when(agendamentoRepository.findByCodigoAcesso("codigo-secreto")).thenReturn(Optional.of(agendamento));
        when(profissionalRepository.findByIdParaAtualizar(1L)).thenReturn(Optional.of(ana));
        when(agendamentoRepository.buscarOcupados(any(), any(), any(), any())).thenReturn(List.of());

        AgendamentoClienteResponse resposta = service.remarcarPeloCliente("codigo-secreto", SEGUNDA.atTime(16, 0));

        assertThat(resposta.inicio()).isEqualTo(SEGUNDA.atTime(16, 0));
        assertThat(resposta.fim()).isEqualTo(SEGUNDA.atTime(16, 30));
        assertThat(agendamento.isLembreteEnviado()).isFalse();
        verify(eventos).publishEvent(any(AgendamentoAlteradoEvent.class));
    }

    @Test
    void remarcarIgnoraOProprioHorarioMasNaoODeOutroCliente() {
        Agendamento agendamento = agendamentoDoCliente();
        Agendamento outro = new Agendamento();
        outro.setId(8L);
        outro.setInicio(SEGUNDA.atTime(15, 0));
        outro.setFim(SEGUNDA.atTime(15, 30));
        when(agendamentoRepository.findByCodigoAcesso("codigo-secreto")).thenReturn(Optional.of(agendamento));
        when(profissionalRepository.findByIdParaAtualizar(1L)).thenReturn(Optional.of(ana));
        when(agendamentoRepository.buscarOcupados(any(), any(), any(), any()))
                .thenReturn(List.of(agendamento))   // 14:00 -> 14:00 de novo: só ele mesmo
                .thenReturn(List.of(outro));        // 15:00: horário de outro cliente

        assertThat(service.remarcarPeloCliente("codigo-secreto", SEGUNDA.atTime(14, 0)).inicio())
                .isEqualTo(SEGUNDA.atTime(14, 0));
        assertThatThrownBy(() -> service.remarcarPeloCliente("codigo-secreto", SEGUNDA.atTime(15, 0)))
                .isInstanceOf(ConflitoHorarioException.class);
    }

    // ---------- Equipe: cada profissional só mexe nos próprios horários ----------

    @Test
    void profissionalNaoConcluiAgendamentoDeOutraPessoa() {
        when(agendamentoRepository.findById(7L)).thenReturn(Optional.of(agendamentoDoCliente()));

        assertThatThrownBy(() -> service.concluir(7L, 99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(service.concluir(7L, 1L).status()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }
}
