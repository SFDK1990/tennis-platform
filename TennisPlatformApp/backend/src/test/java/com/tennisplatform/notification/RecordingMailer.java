package com.tennisplatform.notification;

import com.tennisplatform.notification.application.port.out.Mail;
import com.tennisplatform.notification.application.port.out.Mailer;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the booking emails instead of sending them. Every integration test gets it
 * ({@code AbstractIntegrationTest}), so no test waits on an SMTP server and the context stays
 * the same one for the whole run. {@code SmtpMailer} is covered on its own.
 */
@Primary
public class RecordingMailer implements Mailer {

    private final List<Mail> sent = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void send(Mail mail) {
        sent.add(mail);
    }

    public List<Mail> sent() {
        synchronized (sent) {
            return List.copyOf(sent);
        }
    }

    public List<Mail> to(String address) {
        return sent().stream().filter(mail -> mail.to().equals(address)).toList();
    }

    public void clear() {
        sent.clear();
    }
}
