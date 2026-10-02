package senac.com.backendTCG.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envia pelo SMTP configurado (SPRING_MAIL_HOST...). Sem SMTP, o email so e escrito no log,
// o que permite testar a redefinicao de senha localmente.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.email.remetente}")
    private String remetente;

    public void enviar(String para, String assunto, String texto) {
        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            log.info("[EMAIL] SMTP não configurado, email não enviado.\nPara: {}\nAssunto: {}\n{}", para, assunto, texto);
            return;
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(para);
            mensagem.setSubject(assunto);
            mensagem.setText(texto);
            sender.send(mensagem);
        } catch (Exception e) {
            // Falha no envio nao derruba a requisicao: o usuario pode pedir o reenvio
            log.error("Falha ao enviar email para {}", para, e);
        }
    }
}
