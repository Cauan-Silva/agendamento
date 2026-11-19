package com.cauan.agendamento.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TratadorDeErrosTest {

    @Test
    void muitasTentativasViram429ComRetryAfter() {
        ResponseEntity<ProblemDetail> resposta = new TratadorDeErros()
                .muitasTentativas(new MuitasTentativasException(Duration.ofSeconds(90)));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("90");
        assertThat(resposta.getBody().getDetail()).contains("Muitas tentativas").contains("2 minutos");
    }
}
