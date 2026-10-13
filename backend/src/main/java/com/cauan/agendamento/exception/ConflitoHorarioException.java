package com.cauan.agendamento.exception;

/** O horário pedido já está ocupado (HTTP 409). */
public class ConflitoHorarioException extends RuntimeException {

    public ConflitoHorarioException(String mensagem) {
        super(mensagem);
    }
}
