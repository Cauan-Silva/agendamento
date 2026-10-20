package com.cauan.agendamento.notificacao;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envia e-mails de confirmação e lembrete.
 *
 * Se o SMTP não estiver configurado (variáveis SPRING_MAIL_*), o Spring não cria o
 * JavaMailSender e as mensagens só aparecem no log — a API continua funcionando.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;
    private final String nomeNegocio;
    private final String urlSite;

    public NotificacaoService(ObjectProvider<JavaMailSender> mailSender,
                              @Value("${agenda.email.remetente}") String remetente,
                              @Value("${agenda.nome-negocio}") String nomeNegocio,
                              @Value("${agenda.url-site}") String urlSite) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.nomeNegocio = nomeNegocio;
        this.urlSite = urlSite.endsWith("/") ? urlSite.substring(0, urlSite.length() - 1) : urlSite;
    }

    /** Página onde o cliente consulta, cancela ou remarca o horário, sem login. */
    String linkDoCliente(AgendamentoResponse a) {
        return urlSite + "/#/meu-agendamento/" + a.codigoAcesso();
    }

    /**
     * Roda depois que o agendamento foi salvo de fato (após o commit) e em outra
     * thread, para o cliente não esperar o e-mail sair para ver o comprovante.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCriarAgendamento(AgendamentoCriadoEvent evento) {
        AgendamentoResponse a = evento.agendamento();
        enviar(a.clienteEmail(),
                "Horário confirmado: " + a.servicoNome() + " em " + nomeNegocio,
                """
                Olá, %s!

                Seu horário está confirmado:

                %s com %s
                %s, às %s
                Número do agendamento: %04d

                Precisa cancelar ou trocar o horário? Use este link:
                %s

                Até lá!
                %s
                """.formatted(a.clienteNome(), a.servicoNome(), a.profissionalNome(),
                        capitalizar(a.inicio().format(DATA)), a.inicio().format(HORA), a.id(),
                        linkDoCliente(a), nomeNegocio));
    }

    /** Confirmação para o cliente depois que ele mesmo remarca ou cancela pelo link. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAlterarAgendamento(AgendamentoAlteradoEvent evento) {
        AgendamentoResponse a = evento.agendamento();
        if (evento.alteracao() == AgendamentoAlteradoEvent.Alteracao.CANCELADO) {
            enviar(a.clienteEmail(),
                    "Horário cancelado em " + nomeNegocio,
                    """
                    Olá, %s!

                    Seu horário de %s com %s em %s, às %s, foi cancelado.

                    Quando quiser, é só agendar de novo:
                    %s

                    %s
                    """.formatted(a.clienteNome(), a.servicoNome(), a.profissionalNome(),
                            a.inicio().format(DATA), a.inicio().format(HORA), urlSite, nomeNegocio));
            return;
        }
        enviar(a.clienteEmail(),
                "Horário remarcado: " + a.servicoNome() + " em " + nomeNegocio,
                """
                Olá, %s!

                Seu horário foi remarcado para:

                %s com %s
                %s, às %s
                Número do agendamento: %04d

                Para cancelar ou trocar de novo:
                %s

                %s
                """.formatted(a.clienteNome(), a.servicoNome(), a.profissionalNome(),
                        capitalizar(a.inicio().format(DATA)), a.inicio().format(HORA), a.id(),
                        linkDoCliente(a), nomeNegocio));
    }

    /** Usado pelo LembreteService. Devolve true se o e-mail saiu (ou foi registrado no log). */
    public boolean enviarLembrete(AgendamentoResponse a) {
        return enviar(a.clienteEmail(),
                "Lembrete: amanhã às " + a.inicio().format(HORA) + " em " + nomeNegocio,
                """
                Olá, %s!

                Passando para lembrar do seu horário de amanhã:

                %s com %s
                %s, às %s

                Se não puder ir, cancele ou troque o horário por este link,
                para liberar a vaga para outra pessoa:
                %s

                %s
                """.formatted(a.clienteNome(), a.servicoNome(), a.profissionalNome(),
                        capitalizar(a.inicio().format(DATA)), a.inicio().format(HORA),
                        linkDoCliente(a), nomeNegocio));
    }

    private boolean enviar(String para, String assunto, String texto) {
        if (para == null || para.isBlank()) {
            return false;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("E-mail não configurado. Mensagem que seria enviada para {}: {}", para, assunto);
            return true;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        if (remetente != null && !remetente.isBlank()) {
            mensagem.setFrom(remetente);
        }
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        try {
            sender.send(mensagem);
            return true;
        } catch (MailException e) {
            log.warn("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
            return false;
        }
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
