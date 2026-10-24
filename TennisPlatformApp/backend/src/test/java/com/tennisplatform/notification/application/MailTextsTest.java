package com.tennisplatform.notification.application;

import com.tennisplatform.notification.application.port.out.Mail;
import com.tennisplatform.notification.application.service.MailTexts;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MailTextsTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final UUID BOOKING = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final MailTexts TEXTS = new MailTexts("Escuela de Tenis Oasis Papagayo", "no-reply@example.com",
            "https://app.example.com", Clock.fixed(Instant.parse("2026-03-20T10:00:00Z"), ZoneOffset.UTC));

    private static MailTexts.Lesson lesson(String startsAt, String endsAt, String type) {
        return new MailTexts.Lesson(BOOKING, UUID.randomUUID(), type, Instant.parse(startsAt), Instant.parse(endsAt),
                MADRID, "Ana Serrano");
    }

    @Test
    void theLessonIsDescribedOnTheTeachersWallClockAlsoOnTheDayTheClocksGoForward() {
        // 29 March 2026: Madrid moves from UTC+1 to UTC+2 at 01:00 UTC.
        Mail mail = TEXTS.confirmationToStudent(lesson("2026-03-29T08:00:00Z", "2026-03-29T09:00:00Z", "INDIVIDUAL"),
                "lucia@example.com", "Lucia Prieto");

        assertThat(mail.subject()).isEqualTo("Reserva confirmada: domingo 29 de marzo, 10:00");
        assertThat(mail.body()).startsWith("Hola, Lucia Prieto:")
                .contains("clase individual del domingo 29 de marzo, de 10:00 a 11:00")
                .contains("https://app.example.com/bookings")
                .endsWith("Escuela de Tenis Oasis Papagayo\n");
        assertThat(mail.calendar().method()).isEqualTo("REQUEST");
    }

    @Test
    void aStudentWithoutANameIsGreetedAnyway() {
        Mail mail = TEXTS.confirmationToStudent(lesson("2026-03-27T09:00:00Z", "2026-03-27T10:00:00Z", "GROUP"),
                "lucia@example.com", null);

        assertThat(mail.body()).startsWith("Hola:\n\n").contains("clase grupal del viernes 27 de marzo, de 10:00");
    }

    @Test
    void theCancellationSaysWhoAndWhyAndCarriesTheCalendarCancellation() {
        Mail mail = TEXTS.cancellationToStudent(lesson("2026-03-27T09:00:00Z", "2026-03-27T10:00:00Z", "GROUP"),
                "lucia@example.com", "Lucia", "Ana Serrano", "  Lluvia  ");

        assertThat(mail.subject()).isEqualTo("Clase cancelada: viernes 27 de marzo, 10:00");
        assertThat(mail.body()).contains("Ana Serrano ha cancelado tu clase del viernes 27 de marzo, de 10:00 a 11:00.")
                .contains("Motivo: Lluvia\n");
        assertThat(mail.calendar().method()).isEqualTo("CANCEL");
        assertThat(mail.calendar().content()).contains("UID:" + BOOKING + "@tennis-platform", "DESCRIPTION:Lluvia");
    }

    @Test
    void aCancellationByTheAdministrationWithoutAReasonSaysSo() {
        Mail mail = TEXTS.cancellationToStudent(lesson("2026-03-27T09:00:00Z", "2026-03-27T10:00:00Z", "GROUP"),
                "lucia@example.com", "Lucia", null, null);

        assertThat(mail.body()).contains("La administración de la escuela ha cancelado tu clase")
                .doesNotContain("Motivo");
        assertThat(mail.calendar().content()).doesNotContain("DESCRIPTION");
    }

    @Test
    void theTeacherHearsWhoBookedAndHowFullTheLessonIs() {
        Mail mail = TEXTS.bookingToTeacher(lesson("2026-03-27T09:00:00Z", "2026-03-27T10:00:00Z", "GROUP"),
                "ana@example.com", "Lucia Prieto", 3, 6, false);

        assertThat(mail.subject()).isEqualTo("Nueva reserva: Lucia Prieto, viernes 27 de marzo, 10:00");
        assertThat(mail.body()).contains("Plazas ocupadas: 3 de 6.");
        assertThat(mail.calendar()).isNull();
    }
}
