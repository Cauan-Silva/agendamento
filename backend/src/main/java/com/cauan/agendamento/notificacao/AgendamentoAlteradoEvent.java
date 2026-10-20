package com.cauan.agendamento.notificacao;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;

/** Publicado quando o próprio cliente remarca ou cancela pelo link do e-mail. */
public record AgendamentoAlteradoEvent(AgendamentoResponse agendamento, Alteracao alteracao) {

    public enum Alteracao { REMARCADO, CANCELADO }
}
