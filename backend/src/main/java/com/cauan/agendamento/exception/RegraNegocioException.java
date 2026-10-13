package com.cauan.agendamento.exception;

/** Requisição válida no formato, mas que quebra uma regra do negócio (HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
