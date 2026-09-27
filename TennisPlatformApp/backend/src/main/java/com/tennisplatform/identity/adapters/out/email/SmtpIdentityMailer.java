package com.tennisplatform.identity.adapters.out.email;

import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.configuration.IdentityProperties;
import com.tennisplatform.identity.domain.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
class SmtpIdentityMailer implements IdentityMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpIdentityMailer.class);

    private final JavaMailSender mailSender;
    private final IdentityProperties properties;

    SmtpIdentityMailer(JavaMailSender mailSender, IdentityProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendEmailVerification(EmailAddress recipient, String rawToken) {
        send(recipient, "Confirma tu correo",
                "Confirma tu cuenta en Tennis Platform abriendo este enlace:\n\n"
                        + link("/verify-email", rawToken)
                        + "\n\nSi no has creado ninguna cuenta, ignora este mensaje.");
    }

    @Override
    public void sendPasswordReset(EmailAddress recipient, String rawToken) {
        send(recipient, "Restablece tu contraseña",
                "Puedes establecer una contraseña nueva en este enlace:\n\n"
                        + link("/reset-password", rawToken)
                        + "\n\nEl enlace caduca en " + properties.getPasswordResetTtl().toHours()
                        + " hora(s) y solo puede usarse una vez."
                        + "\n\nSi no has solicitado el cambio, ignora este mensaje: tu contraseña no cambiará.");
    }

    @Override
    public void sendRegistrationAttemptOnExistingAccount(EmailAddress recipient) {
        send(recipient, "Intento de registro con tu correo",
                "Alguien ha intentado crear una cuenta en Tennis Platform con esta dirección, "
                        + "que ya está registrada. No se ha creado ninguna cuenta nueva ni se ha "
                        + "modificado la tuya.\n\nSi has sido tú, puedes iniciar sesión con normalidad "
                        + "o restablecer tu contraseña si no la recuerdas.");
    }

    private String link(String path, String rawToken) {
        return properties.getFrontendBaseUrl() + path + "?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private void send(EmailAddress recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom());
        message.setTo(recipient.value());
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // A mail outage must not turn into a failed registration that rolls back the
            // account. Only the kind of failure is logged, not the exception: an SMTP server
            // that rejects a recipient quotes the address in its message, and that is personal
            // data (28-fase16-analisis-observabilidad.md).
            log.error("Could not send '{}' email: {}", subject,
                    NestedExceptionUtils.getMostSpecificCause(e).getClass().getName());
        }
    }
}
