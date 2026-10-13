package com.cauan.agendamento.notificacao;

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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LembreteServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private AgendamentoRepository repository;
    @Mock
    private NotificacaoService notificacao;

    @Test
    void enviaLembreteDosAgendamentosDeAmanhaEMarcaComoEnviado() {
        LocalDateTime hoje18h = LocalDateTime.of(2026, 10, 13, 18, 0);
        Clock relogio = Clock.fixed(hoje18h.atZone(FUSO).toInstant(), FUSO);

        Profissional ana = new Profissional();
        ana.setId(1L);
        ana.setNome("Ana");
        Servico corte = new Servico("Corte", 30, new BigDecimal("45.00"));
        corte.setId(1L);

        Agendamento amanha = new Agendamento();
        amanha.setId(7L);
        amanha.setProfissional(ana);
        amanha.setServico(corte);
        amanha.setValor(new BigDecimal("45.00"));
        amanha.setClienteNome("João");
        amanha.setClienteEmail("joao@email.com");
        amanha.setInicio(LocalDateTime.of(2026, 10, 14, 9, 0));
        amanha.setFim(LocalDateTime.of(2026, 10, 14, 9, 30));
        amanha.setStatus(StatusAgendamento.AGENDADO);

        when(repository.buscarParaLembrete(eq(LocalDateTime.of(2026, 10, 14, 0, 0)), any(), any()))
                .thenReturn(List.of(amanha));
        when(notificacao.enviarLembrete(any())).thenReturn(true);

        new LembreteService(repository, notificacao, relogio).enviarLembretesDeAmanha();

        assertThat(amanha.isLembreteEnviado()).isTrue();
    }
}
