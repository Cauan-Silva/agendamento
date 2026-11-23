package com.cauan.agendamento.exception;

import java.time.Duration;

/** Login bloqueado por um tempo depois de muitas senhas erradas (vira HTTP 429). */
public class MuitasTentativasException extends RuntimeException {

    private final Duration espera;

    public MuitasTentativasException(Duration espera) {
        super("Muitas tentativas de login com senha errada. Tente de novo em "
                + minutos(espera) + ".");
        this.espera = espera;
    }

    /** Quanto falta para poder tentar de novo. */
    public Duration getEspera() {
        return espera;
    }

    private static String minutos(Duration espera) {
        long minutos = Math.max(1, (espera.getSeconds() + 59) / 60); // arredonda para cima
        return minutos == 1 ? "1 minuto" : minutos + " minutos";
    }
}
