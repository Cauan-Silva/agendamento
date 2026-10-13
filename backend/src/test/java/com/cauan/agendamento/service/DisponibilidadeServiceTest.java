package com.cauan.agendamento.service;

import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.Bloqueio;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.model.StatusAgendamento;
import com.cauan.agendamento.repository.AgendamentoRepository;
import com.cauan.agendamento.repository.BloqueioRepository;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.ServicoRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 19);
    private static final LocalDate TERCA = LocalDate.of(2026, 10, 20);

    @Mock
    private ProfissionalRepository profissionalRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private BloqueioRepository bloqueioRepository;

    private Profissional ana;
    private Servico servico60min;

    @BeforeEach
    void setUp() {
        servico60min = new Servico("Corte + barba", 60, new BigDecimal("70.00"));
        servico60min.setId(10L);

        ana = new Profissional();
        ana.setId(1L);
        ana.setNome("Ana");
        ana.setHoraInicio(LocalTime.of(9, 0));
        ana.setHoraFim(LocalTime.of(12, 0));
        ana.setDiasTrabalho(Set.of(DayOfWeek.MONDAY));
        ana.setServicos(Set.of(servico60min));
    }

    private DisponibilidadeService servicoComAgora(LocalDateTime agora) {
        Clock relogio = Clock.fixed(agora.atZone(FUSO).toInstant(), FUSO);
        return new DisponibilidadeService(profissionalRepository, servicoRepository,
                agendamentoRepository, bloqueioRepository, relogio, 30);
    }

    private void semAgendamentos() {
        when(agendamentoRepository.buscarOcupados(eq(1L), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of());
    }

    @Test
    void diaLivreRetornaTodosOsHorariosQueCabemNoExpediente() {
        semAgendamentos();
        DisponibilidadeService service = servicoComAgora(LocalDateTime.of(2026, 10, 12, 8, 0));

        List<LocalTime> horarios = service.calcularHorarios(ana, servico60min, SEGUNDA);

        // Serviço de 60 min, expediente 09:00–12:00, passos de 30 min: o último início possível é 11:00
        assertThat(horarios).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
                LocalTime.of(10, 30), LocalTime.of(11, 0));
    }

    @Test
    void removeHorariosQueSobrepoemAgendamentoExistente() {
        Agendamento existente = new Agendamento();
        existente.setInicio(SEGUNDA.atTime(10, 0));
        existente.setFim(SEGUNDA.atTime(11, 0));
        when(agendamentoRepository.buscarOcupados(eq(1L), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of(existente));
        DisponibilidadeService service = servicoComAgora(LocalDateTime.of(2026, 10, 12, 8, 0));

        List<LocalTime> horarios = service.calcularHorarios(ana, servico60min, SEGUNDA);

        // 09:30 (até 10:30), 10:00 e 10:30 (até 11:30) batem com o agendamento das 10:00–11:00
        assertThat(horarios).containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    @Test
    void naoMostraHorariosQueJaPassaram() {
        semAgendamentos();
        DisponibilidadeService service = servicoComAgora(SEGUNDA.atTime(10, 15));

        List<LocalTime> horarios = service.calcularHorarios(ana, servico60min, SEGUNDA);

        assertThat(horarios).containsExactly(LocalTime.of(10, 30), LocalTime.of(11, 0));
    }

    @Test
    void removeHorariosQueCaemNoAlmoco() {
        semAgendamentos();
        ana.setAlmocoInicio(LocalTime.of(10, 30));
        ana.setAlmocoFim(LocalTime.of(11, 0));
        DisponibilidadeService service = servicoComAgora(LocalDateTime.of(2026, 10, 12, 8, 0));

        List<LocalTime> horarios = service.calcularHorarios(ana, servico60min, SEGUNDA);

        // 10:00 (até 11:00) e 10:30 (até 11:30) pegam o almoço das 10:30–11:00
        assertThat(horarios).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(11, 0));
    }

    @Test
    void removeHorariosDentroDeUmBloqueio() {
        semAgendamentos();
        Bloqueio folga = new Bloqueio();
        folga.setInicio(SEGUNDA.atTime(9, 0));
        folga.setFim(SEGUNDA.atTime(10, 0));
        when(bloqueioRepository.buscarNoPeriodo(eq(1L), any(), any())).thenReturn(List.of(folga));
        DisponibilidadeService service = servicoComAgora(LocalDateTime.of(2026, 10, 12, 8, 0));

        List<LocalTime> horarios = service.calcularHorarios(ana, servico60min, SEGUNDA);

        assertThat(horarios).containsExactly(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0));
    }

    @Test
    void feriadoBloqueiaODiaInteiro() {
        semAgendamentos();
        Bloqueio feriado = new Bloqueio();
        feriado.setInicio(SEGUNDA.atStartOfDay());
        feriado.setFim(SEGUNDA.atTime(23, 59));
        when(bloqueioRepository.buscarNoPeriodo(eq(1L), any(), any())).thenReturn(List.of(feriado));
        DisponibilidadeService service = servicoComAgora(LocalDateTime.of(2026, 10, 12, 8, 0));

        assertThat(service.calcularHorarios(ana, servico60min, SEGUNDA)).isEmpty();
    }

    @Test
    void diaEmQueOProfissionalNaoTrabalhaNaoTemHorarios() {
        DisponibilidadeService service = servicoComAgora(LocalDateTime.of(2026, 10, 12, 8, 0));

        List<LocalTime> horarios = service.calcularHorarios(ana, servico60min, TERCA);

        assertThat(horarios).isEmpty();
        verifyNoInteractions(agendamentoRepository);
    }

    @Test
    void recusaServicoQueOProfissionalNaoRealiza() {
        Servico outro = new Servico("Manicure", 45, new BigDecimal("40.00"));
        outro.setId(99L);

        assertThatThrownBy(() -> DisponibilidadeService.validarCombinacao(ana, outro))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não realiza");
    }
}
