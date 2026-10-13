package com.cauan.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas no formato padrão RFC 7807 (application/problem+json).
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflitoHorarioException.class)
    public ProblemDetail conflito(ConflitoHorarioException e) {
        return problema(HttpStatus.CONFLICT, "Horário indisponível", e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio", e.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciais(CredenciaisInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Login inválido", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos");
        problema.setProperty("campos", campos);
        return problema;
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
