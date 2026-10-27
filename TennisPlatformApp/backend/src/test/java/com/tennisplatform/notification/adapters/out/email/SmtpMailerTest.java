package com.tennisplatform.notification.adapters.out.email;

import com.tennisplatform.notification.application.port.out.Mail;
import com.tennisplatform.notification.configuration.NotificationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class SmtpMailerTest {

    private static final String ADDRESS = "lucia@example.com";

    private JavaMailSender sender;
    private SimpleMeterRegistry meters;
    private SmtpMailer mailer;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        meters = new SimpleMeterRegistry();
        NotificationProperties properties = new NotificationProperties();
        properties.setMailFrom("no-reply@example.com");
        properties.setSchoolName("Escuela de Tenis Oasis Papagayo");
        mailer = new SmtpMailer(sender, properties, meters);
    }

    private static Mail mail() {
        return new Mail("booking-confirmed-student", "b-1", ADDRESS, "Reserva confirmada", "Hola",
                new Mail.Calendar("REQUEST", "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"));
    }

    @Test
    void theLessonTravelsAsACalendarAttachmentFromTheSchool() throws Exception {
        mailer.send(mail());

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        MimeMessage message = sent.getValue();
        // What JavaMailSender does before sending: it is what writes each part's Content-Type.
        message.saveChanges();
        assertThat(((InternetAddress) message.getFrom()[0]).getPersonal()).isEqualTo("Escuela de Tenis Oasis Papagayo");
        Multipart parts = (Multipart) message.getContent();
        assertThat(parts.getCount()).isEqualTo(2);
        assertThat(parts.getBodyPart(1).getContentType()).contains("text/calendar", "method=REQUEST");
        assertThat(parts.getBodyPart(1).getFileName()).isEqualTo("clase.ics");
        assertThat(meters.counter("tennis.mail.sent", "kind", "booking-confirmed-student", "outcome", "sent").count())
                .isEqualTo(1);
    }

    @Test
    void aFailureIsCountedAndLoggedWithoutTheAddressAndNeverThrown(CapturedOutput output) {
        doThrow(new MailSendException("550 5.1.1 <" + ADDRESS + ">: Recipient address rejected"))
                .when(sender).send(any(MimeMessage.class));

        mailer.send(mail());

        assertThat(meters.counter("tennis.mail.sent", "kind", "booking-confirmed-student", "outcome", "failed").count())
                .isEqualTo(1);
        assertThat(output.getAll()).contains("Could not send booking-confirmed-student b-1")
                .doesNotContain(ADDRESS);
    }
}
