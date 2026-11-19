package com.cauan.agendamento.security;

import com.cauan.agendamento.exception.MuitasTentativasException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra "chutar" senhas: depois de {@value #MAX_FALHAS} senhas erradas para o mesmo
 * login, vindas do mesmo IP, dentro de 15 minutos, o login fica bloqueado até a janela passar.
 * Um login certo zera a contagem.
 *
 * Fica só na memória (sem dependências novas): reiniciar a API zera tudo, e com mais de uma
 * instância cada uma conta separado. Para não crescer sem limite, guarda no máximo
 * {@value #MAX_REGISTROS} registros e descarta os vencidos.
 */
@Component
public class LimitadorTentativasLogin {

    static final int MAX_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final int MAX_REGISTROS = 10_000;

    /** Falhas seguidas desde {@code inicio} (a primeira falha da janela). */
    private record Registro(Instant inicio, int falhas) {
        Instant fim() {
            return inicio.plus(JANELA);
        }
    }

    private final Clock clock;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public LimitadorTentativasLogin(Clock clock) {
        this.clock = clock;
    }

    /** Lança {@link MuitasTentativasException} se esse login (deste IP) está bloqueado agora. */
    public void verificar(String login, String ip) {
        Instant agora = clock.instant();
        Registro registro = registros.get(chave(login, ip));
        if (registro != null && registro.falhas() >= MAX_FALHAS && agora.isBefore(registro.fim())) {
            throw new MuitasTentativasException(Duration.between(agora, registro.fim()));
        }
    }

    public void registrarFalha(String login, String ip) {
        Instant agora = clock.instant();
        String chave = chave(login, ip);
        if (registros.size() >= MAX_REGISTROS && !registros.containsKey(chave)) {
            abrirEspaco(agora);
        }
        registros.compute(chave, (k, atual) -> atual == null || !agora.isBefore(atual.fim())
                ? new Registro(agora, 1)
                : new Registro(atual.inicio(), atual.falhas() + 1));
    }

    public void registrarSucesso(String login, String ip) {
        registros.remove(chave(login, ip));
    }

    /** Quantos registros estão na memória (para os testes). */
    int tamanho() {
        return registros.size();
    }

    /** Tira os vencidos; se ainda estiver cheio (muitos logins diferentes), tira o mais antigo. */
    private void abrirEspaco(Instant agora) {
        registros.values().removeIf(r -> !agora.isBefore(r.fim()));
        while (registros.size() >= MAX_REGISTROS) {
            registros.entrySet().stream()
                    .min(Comparator.comparing(e -> e.getValue().inicio()))
                    .ifPresent(e -> registros.remove(e.getKey(), e.getValue()));
        }
    }

    private static String chave(String login, String ip) {
        return login.trim().toLowerCase(Locale.ROOT) + "|" + ip;
    }
}
