package com.tennisplatform.identity.adapters.out.email;

import com.tennisplatform.identity.configuration.IdentityProperties;
import com.tennisplatform.identity.domain.EmailAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** The account emails come from the school, like the booking ones (Fase 20). */
class SmtpIdentityMailerTest {

    @Test
    void theVerificationEmailComesFromTheSchoolAndNamesIt() {
        JavaMailSender sender = mock(JavaMailSender.class);
        IdentityProperties properties = new IdentityProperties();
        properties.setSchoolName("Escuela de Tenis Oasis Papagayo");

        new SmtpIdentityMailer(sender, properties).sendEmailVerification(new EmailAddress("lucia@example.com"), "t0ken");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        assertThat(sent.getValue().getFrom()).isEqualTo("Escuela de Tenis Oasis Papagayo <no-reply@tennis-platform.local>");
        assertThat(sent.getValue().getText()).contains("Confirma tu cuenta en Escuela de Tenis Oasis Papagayo")
                .contains("/verify-email?token=t0ken")
                .doesNotContain("Tennis Platform");
    }
}
