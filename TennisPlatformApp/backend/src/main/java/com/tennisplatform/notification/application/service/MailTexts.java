package com.tennisplatform.notification.application.service;

import com.tennisplatform.notification.application.port.out.Mail;
import com.tennisplatform.notification.domain.CalendarInvite;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * What each email says: plain text, in Spanish, with the lesson on the teacher's wall clock,
 * which is how the student sees it in the app.
 */
public class MailTexts {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", SPANISH);

    private final String schoolName;
    private final String schoolAddress;
    private final String frontendBaseUrl;
    private final Clock clock;

    /**
     * @param schoolAddress the sender address, which is also the organizer of the calendar events
     */
    public MailTexts(String schoolName, String schoolAddress, String frontendBaseUrl, Clock clock) {
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.frontendBaseUrl = frontendBaseUrl;
        this.clock = clock;
    }

    /** A lesson as the emails describe it. */
    public record Lesson(UUID bookingId, UUID lessonId, String type, Instant startsAt, Instant endsAt, ZoneId zone,
                         String teacherName) {

        String day() {
            return DAY.format(startsAt.atZone(zone));
        }

        String start() {
            return TIME.format(startsAt.atZone(zone));
        }

        String end() {
            return TIME.format(endsAt.atZone(zone));
        }

        String kind() {
            return "GROUP".equals(type) ? "grupal" : "individual";
        }
    }

    public Mail confirmationToStudent(Lesson lesson, String to, String studentName) {
        String body = greeting(studentName)
                + "Tienes plaza en la clase " + lesson.kind() + " del " + lesson.day()
                + ", de " + lesson.start() + " a " + lesson.end() + ".\n\n"
                + "Te la adjuntamos para que la añadas a tu calendario.\n\n"
                + "Si no puedes venir, cancélala en " + frontendBaseUrl + "/bookings"
                + " con más de 24 horas de antelación.\n\n"
                + signature();
        return new Mail("booking-confirmed-student", lesson.bookingId().toString(), to,
                "Reserva confirmada: " + lesson.day() + ", " + lesson.start(), body,
                calendar(CalendarInvite.Method.REQUEST, lesson, null));
    }

    /**
     * The seat came from the waiting list (Fase 21): the student did not book it just now, so the
     * email says how it happened and how to give it up.
     */
    public Mail waitingListSeatToStudent(Lesson lesson, String to, String studentName) {
        String body = greeting(studentName)
                + "Se ha liberado una plaza en la clase " + lesson.kind() + " del " + lesson.day()
                + ", de " + lesson.start() + " a " + lesson.end() + ", y es para ti: estabas en el"
                + " primer puesto de la lista de espera.\n\n"
                + "Te la adjuntamos para que la añadas a tu calendario.\n\n"
                + "Si ya no puedes venir, cancélala en " + frontendBaseUrl + "/bookings"
                + " con más de 24 horas de antelación.\n\n"
                + signature();
        return new Mail("booking-confirmed-student-waiting-list", lesson.bookingId().toString(), to,
                "Tienes plaza: " + lesson.day() + ", " + lesson.start(), body,
                calendar(CalendarInvite.Method.REQUEST, lesson, null));
    }

    public Mail bookingToTeacher(Lesson lesson, String to, String student, int booked, int capacity,
                                 boolean fromWaitingList) {
        String body = student + (fromWaitingList ? " ha conseguido plaza, desde la lista de espera, en" : " ha reservado")
                + " la clase " + lesson.kind() + " del " + lesson.day()
                + ", de " + lesson.start() + " a " + lesson.end() + ".\n\n"
                + "Plazas ocupadas: " + booked + " de " + capacity + ".\n\n"
                + frontendBaseUrl + "/teacher/lessons/" + lesson.lessonId() + "\n";
        return new Mail("booking-confirmed-teacher", lesson.bookingId().toString(), to,
                "Nueva reserva: " + student + ", " + lesson.day() + ", " + lesson.start(), body, null);
    }

    public Mail studentCancellationToTeacher(Lesson lesson, String to, String student) {
        String body = student + " ha cancelado su reserva en la clase " + lesson.kind() + " del " + lesson.day()
                + ", de " + lesson.start() + " a " + lesson.end() + ". La plaza vuelve a estar libre.\n\n"
                + frontendBaseUrl + "/teacher/lessons/" + lesson.lessonId() + "\n";
        return new Mail("booking-cancelled-teacher", lesson.bookingId().toString(), to,
                "Reserva cancelada: " + student + ", " + lesson.day() + ", " + lesson.start(), body, null);
    }

    /**
     * @param byWhom the teacher's name, or null when it was the administration
     * @param reason what the teacher wrote, or null
     */
    public Mail cancellationToStudent(Lesson lesson, String to, String studentName, String byWhom, String reason) {
        String who = byWhom != null ? byWhom : "La administración de la escuela";
        String why = reason == null || reason.isBlank() ? null : reason.strip();
        String body = greeting(studentName)
                + who + " ha cancelado tu clase del " + lesson.day()
                + ", de " + lesson.start() + " a " + lesson.end() + ".\n\n"
                + (why != null ? "Motivo: " + why + "\n\n" : "")
                + "No tienes que hacer nada: la reserva ya no cuenta, y el adjunto la quita de tu calendario.\n\n"
                + signature();
        return new Mail("booking-cancelled-student", lesson.bookingId().toString(), to,
                "Clase cancelada: " + lesson.day() + ", " + lesson.start(), body,
                calendar(CalendarInvite.Method.CANCEL, lesson, why));
    }

    /**
     * The student was first on the waiting list when a seat came free, but had no lesson left on
     * their passes (Fase 27): the seat went to the next one, and they are off the list.
     */
    public Mail waitingPlaceLostToStudent(Lesson lesson, String to, String studentName) {
        String body = greeting(studentName)
                + "Se ha liberado una plaza en la clase " + lesson.kind() + " del " + lesson.day()
                + ", de " + lesson.start() + " a " + lesson.end() + ", y te tocaba a ti, pero no te quedan"
                + " clases en tu bono para ella. La plaza ha pasado al siguiente de la lista y ya no estás"
                + " en ella.\n\n"
                + "Cuando " + lesson.teacherName() + " te apunte un bono nuevo, podrás reservar en "
                + frontendBaseUrl + "/calendar\n\n"
                + signature();
        return new Mail("waiting-place-lost-student", lesson.lessonId().toString(), to,
                "Sin bono para la plaza: " + lesson.day() + ", " + lesson.start(), body, null);
    }

    /** The lesson moved to another court (Fase 25.6): the time stays, so only the court is news. */
    public Mail courtChangedToStudent(Lesson lesson, String courtName, String to, String studentName) {
        String body = greeting(studentName)
                + "Tu clase " + lesson.kind() + " del " + lesson.day() + ", de " + lesson.start() + " a " + lesson.end()
                + ", pasa a la " + courtName + ". La hora no cambia.\n\n"
                + "La tienes en " + frontendBaseUrl + "/bookings\n\n"
                + signature();
        return new Mail("lesson-court-changed-student", lesson.lessonId().toString(), to,
                "Cambio de pista: " + lesson.day() + ", " + lesson.start(), body, null);
    }

    /** A message from the teacher (Fase 24): their words as they wrote them, and where else to read them. */
    public Mail messageToStudent(UUID messageId, String subject, String text, String teacherName, String to,
                                 String studentName) {
        String body = greeting(studentName)
                + teacherName + " te ha escrito:\n\n"
                + text + "\n\n"
                + "Lo tienes también en tus avisos: " + frontendBaseUrl + "/messages\n\n"
                + signature();
        return new Mail("message-student", messageId.toString(), to, subject, body, null);
    }

    /**
     * A group the student was signed up to (Fase 28): one email for every lesson booked, with the
     * first one and how to skip a day. {@code firstLesson} is null when no lesson is left yet.
     */
    public Mail joinedGroupToStudent(UUID membershipId, Group group, String to, String studentName, boolean byStudent) {
        String who = byStudent ? "Te has apuntado" : group.teacherName() + " te ha apuntado";
        String lessons = group.firstLesson() == null
                ? "Aún no tiene clases por delante: cuando " + group.teacherName() + " las añada, tendrás plaza en ellas."
                : "Tienes plaza en sus " + group.upcomingLessons() + " clases que quedan; la primera, el "
                        + group.firstLesson().day() + " a las " + group.firstLesson().start() + ".";
        String body = greeting(studentName)
                + who + " al grupo «" + group.name() + "», de " + euros(group.monthlyFeeCents()) + " al mes.\n\n"
                + lessons + "\n\n"
                + "Si un día no puedes ir, cancela esa clase como cualquier reserva, con más de 24 horas. La cuota"
                + " se paga a " + group.teacherName() + " cada mes.\n\n"
                + "Tus clases y tus cuotas: " + frontendBaseUrl + "/bookings\n\n"
                + signature();
        return new Mail("group-joined-student", membershipId.toString(), to, "Apuntado al grupo " + group.name(),
                body, null);
    }

    public Mail joinedGroupToTeacher(UUID membershipId, Group group, String to, String student) {
        String body = "Hola:\n\n"
                + student + " se ha apuntado al grupo «" + group.name() + "» y tiene plaza en sus clases.\n\n"
                + "Sus cuotas: " + frontendBaseUrl + "/teacher/fees\n\n"
                + signature();
        return new Mail("group-joined-teacher", membershipId.toString(), to, "Nuevo en " + group.name(), body, null);
    }

    /** The student left: the teacher learns which day their seats are free from. */
    public Mail leftGroupToTeacher(UUID membershipId, Group group, Instant leftAt, String to, String student) {
        String body = "Hola:\n\n"
                + student + " se ha dado de baja del grupo «" + group.name() + "». Sigue en sus clases hasta fin de"
                + " mes; desde el " + DAY.format(leftAt.atZone(group.zone())) + " sus plazas quedan libres.\n\n"
                + signature();
        return new Mail("group-left-teacher", membershipId.toString(), to, "Baja en " + group.name(), body, null);
    }

    /** The teacher took the student out of the group. */
    public Mail leftGroupToStudent(UUID membershipId, Group group, Instant leftAt, String to, String studentName) {
        String body = greeting(studentName)
                + group.teacherName() + " te ha dado de baja del grupo «" + group.name() + "». Sigues en sus clases"
                + " hasta fin de mes; desde el " + DAY.format(leftAt.atZone(group.zone()))
                + " ya no tienes plaza en ellas.\n\n"
                + "Tus reservas: " + frontendBaseUrl + "/bookings\n\n"
                + signature();
        return new Mail("group-left-student", membershipId.toString(), to, "Baja del grupo " + group.name(), body,
                null);
    }

    /** A fixed group as the emails describe it. */
    public record Group(String name, int monthlyFeeCents, int upcomingLessons, Lesson firstLesson, ZoneId zone,
                        String teacherName) {
    }

    private static String euros(int cents) {
        return String.format(SPANISH, "%d,%02d €", cents / 100, cents % 100);
    }

    private Mail.Calendar calendar(CalendarInvite.Method method, Lesson lesson, String reason) {
        String summary = "Clase de tenis con " + lesson.teacherName();
        String content = CalendarInvite.of(method, lesson.bookingId() + "@tennis-platform", lesson.startsAt(),
                lesson.endsAt(), summary, reason, schoolName, schoolAddress, clock.instant());
        return new Mail.Calendar(method.name(), content);
    }

    private static String greeting(String name) {
        return (name != null && !name.isBlank() ? "Hola, " + name.strip() + ":" : "Hola:") + "\n\n";
    }

    private String signature() {
        return schoolName + "\n";
    }
}
