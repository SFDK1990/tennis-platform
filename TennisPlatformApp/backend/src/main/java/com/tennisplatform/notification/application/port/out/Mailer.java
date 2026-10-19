package com.tennisplatform.notification.application.port.out;

/** Sends an email. A failure is logged and counted by the adapter, never thrown. */
public interface Mailer {

    void send(Mail mail);
}
