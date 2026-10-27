package com.tennisplatform.notification.adapters.out.email;

import com.tennisplatform.notification.application.port.out.Mail;
import com.tennisplatform.notification.application.port.out.Mailer;
import com.tennisplatform.notification.configuration.NotificationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends through the same SMTP server as the account emails, with the lesson attached as an
 * iCalendar file when there is one.
 *
 * <p>Counted in {@code tennis.mail.sent} by kind and outcome. Only the kind and the booking or message id
 * are logged: an SMTP server that rejects a recipient quotes the address in its message, which is
 * personal data, so the exception's message never reaches the log either (Fase 16).
 */
@Component
class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);
    private static final String METRIC = "tennis.mail.sent";

    private final JavaMailSender sender;
    private final NotificationProperties properties;
    private final MeterRegistry meters;

    SmtpMailer(JavaMailSender sender, NotificationProperties properties, MeterRegistry meters) {
        this.sender = sender;
        this.properties = properties;
        this.meters = meters;
    }

    @Override
    public void send(Mail mail) {
        try {
            sender.send(message(mail));
            meters.counter(METRIC, "kind", mail.kind(), "outcome", "sent").increment();
            log.info("Sent {} {}", mail.kind(), mail.reference());
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            meters.counter(METRIC, "kind", mail.kind(), "outcome", "failed").increment();
            log.error("Could not send {} {}: {}", mail.kind(), mail.reference(),
                    NestedExceptionUtils.getMostSpecificCause(e).getClass().getName());
        }
    }

    private MimeMessage message(Mail mail) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, mail.calendar() != null,
                StandardCharsets.UTF_8.name());
        helper.setFrom(properties.getMailFrom(), properties.getSchoolName());
        helper.setTo(mail.to());
        helper.setSubject(mail.subject());
        helper.setText(mail.body());
        if (mail.calendar() != null) {
            helper.addAttachment("clase.ics",
                    new ByteArrayResource(mail.calendar().content().getBytes(StandardCharsets.UTF_8)),
                    "text/calendar; charset=UTF-8; method=" + mail.calendar().method());
        }
        return message;
    }
}
