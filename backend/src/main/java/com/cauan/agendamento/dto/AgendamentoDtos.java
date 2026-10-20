package com.cauan.agendamento.dto;

import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.StatusAgendamento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AgendamentoDtos {

    private AgendamentoDtos() {
    }

    /**
     * Exemplo de JSON:
     * { "profissionalId": 1, "servicoId": 2, "inicio": "2026-10-20T09:00:00",
     *   "clienteNome": "João", "clienteTelefone": "48999990000",
     *   "clienteEmail": "joao@email.com" }
     * O e-mail é opcional; com ele o cliente recebe confirmação e lembrete.
     */
    public record AgendamentoRequest(
            @NotNull Long profissionalId,
            @NotNull Long servicoId,
            @NotNull @Future LocalDateTime inicio,
            @NotBlank @Size(max = 100) String clienteNome,
            @NotBlank @Pattern(regexp = "\\d{10,13}", message = "use só números, com DDD") String clienteTelefone,
            @Email(message = "e-mail inválido") @Size(max = 150) String clienteEmail) {
    }

    public record AgendamentoResponse(Long id,
                                      Long profissionalId,
                                      String profissionalNome,
                                      Long servicoId,
                                      String servicoNome,
                                      BigDecimal valor,
                                      String clienteNome,
                                      String clienteTelefone,
                                      String clienteEmail,
                                      LocalDateTime inicio,
                                      LocalDateTime fim,
                                      StatusAgendamento status,
                                      String codigoAcesso) {

        public static AgendamentoResponse de(Agendamento a) {
            return new AgendamentoResponse(
                    a.getId(),
                    a.getProfissional().getId(),
                    a.getProfissional().getNome(),
                    a.getServico().getId(),
                    a.getServico().getNome(),
                    a.getValor(),
                    a.getClienteNome(),
                    a.getClienteTelefone(),
                    a.getClienteEmail(),
                    a.getInicio(),
                    a.getFim(),
                    a.getStatus(),
                    a.getCodigoAcesso());
        }
    }

    /**
     * O que o cliente vê pelo link do e-mail. Não traz telefone nem e-mail,
     * porque o link pode ser encaminhado para outra pessoa.
     */
    public record AgendamentoClienteResponse(Long id,
                                             Long profissionalId,
                                             String profissionalNome,
                                             Long servicoId,
                                             String servicoNome,
                                             BigDecimal valor,
                                             String clienteNome,
                                             LocalDateTime inicio,
                                             LocalDateTime fim,
                                             StatusAgendamento status,
                                             boolean podeAlterar,
                                             LocalDateTime alterarAte) {

        public static AgendamentoClienteResponse de(Agendamento a, boolean podeAlterar, LocalDateTime alterarAte) {
            return new AgendamentoClienteResponse(
                    a.getId(),
                    a.getProfissional().getId(),
                    a.getProfissional().getNome(),
                    a.getServico().getId(),
                    a.getServico().getNome(),
                    a.getValor(),
                    a.getClienteNome(),
                    a.getInicio(),
                    a.getFim(),
                    a.getStatus(),
                    podeAlterar,
                    alterarAte);
        }
    }

    /** Ex.: { "inicio": "2026-10-22T15:00:00" } — mesmo profissional e serviço, outro horário. */
    public record RemarcarRequest(@NotNull @Future LocalDateTime inicio) {
    }

    public record DisponibilidadeResponse(Long profissionalId,
                                          Long servicoId,
                                          LocalDate data,
                                          List<LocalTime> horarios) {
    }
}
