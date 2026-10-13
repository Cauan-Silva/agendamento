package com.cauan.agendamento.notificacao;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;

/**
 * Publicado quando um agendamento é criado. Leva uma cópia dos dados
 * para que quem escuta não precise consultar o banco de novo.
 */
public record AgendamentoCriadoEvent(AgendamentoResponse agendamento) {
}
