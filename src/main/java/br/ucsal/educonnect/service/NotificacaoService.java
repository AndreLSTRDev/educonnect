package br.ucsal.educonnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mails (aviso de mudança de status da denúncia e link de "Esqueci minha senha").
 * Sem servidor SMTP configurado (spring.mail.host vazio), a mensagem é só registrada no log,
 * o que permite testar o fluxo em ambiente de demonstração.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;

    public NotificacaoService(ObjectProvider<JavaMailSender> mailSender,
                              @Value("${educonnect.mail.remetente:nao-responda@educonnect.local}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    /** Envia sem interromper a operação principal se o e-mail falhar. Retorna true se foi enviado. */
    public boolean enviar(String para, String assunto, String texto) {
        if (para == null || para.isBlank()) {
            log.info("E-mail não enviado (usuário sem e-mail cadastrado): {}", assunto);
            return false;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("[E-mail simulado] Para: {} | Assunto: {}\n{}", para, assunto, texto);
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(para);
            msg.setSubject(assunto);
            msg.setText(texto);
            sender.send(msg);
            return true;
        } catch (MailException e) {
            log.warn("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
            return false;
        }
    }
}
