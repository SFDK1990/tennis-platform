package com.tennisplatform.web;

import jakarta.validation.constraints.Size;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.lesson.application.port.in.LessonView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Wire representations of {@code /me}. */
final class MeDtos {

    private MeDtos() {
    }

    /**
     * The account plus whatever personal data the caller's role has.
     *
     * <p>One flat shape for every role, with the fields that do not apply left null, because
     * that is how {@code openapi.yaml} already described it and because the client knows the
     * role - it is right there in the response. The absence of the student fields is
     * meaningful: a verified student who has not filled their data in yet gets nulls, and that
     * is how the frontend knows it has to ask for them
     * (16-fase6-analisis-perfiles.md).
     */
    record MeResponse(UUID id, String email, String role, String status, Instant emailVerifiedAt,
                      String fullName, String phone, String nationalId, String address,
                      String displayName, String timezone) {
    }

    /**
     * Every field optional: this is a PATCH and null means "not submitted".
     *
     * <p>There is no field for the role, the email or the account status. Not an oversight:
     * they are what an attacker would want to change, and a DTO that cannot carry them cannot
     * be tricked into applying them however the body is crafted.
     */
    record UpdateMeRequest(
            @Size(max = 255) String fullName,
            @Size(max = 30) String phone,
            @Size(max = 30) String nationalId,
            @Size(max = 255) String address,
            @Size(max = 255) String displayName,
            @Size(max = 60) String timezone) {
    }

    /**
     * Everything the platform keeps about the caller, as one downloadable file
     * (30-fase19-analisis-cierre-mvp.md). The bookings carry their lesson, so the file can be
     * read without the application; they are empty for the teacher and the administrator.
     */
    record MyDataExport(Instant exportedAt, MeResponse account, List<ExportedBooking> bookings) {
    }

    record ExportedBooking(UUID id, UUID lessonId, UUID studentUserId, String status, String attendance,
                           Instant bookedAt, Instant cancelledAt, ExportedLesson lesson) {

        static ExportedBooking from(BookingView booking) {
            return new ExportedBooking(booking.id(), booking.lessonId(), booking.studentUserId(), booking.status(),
                    booking.attendance(), booking.bookedAt(), booking.cancelledAt(),
                    ExportedLesson.from(booking.lesson()));
        }
    }

    /** The lesson as it travels inside a booking: without the teacher's notes, as everywhere else. */
    record ExportedLesson(UUID id, UUID teacherUserId, String type, Instant startsAt, Instant endsAt,
                          int capacity, int bookedCount, String status) {

        static ExportedLesson from(LessonView lesson) {
            return new ExportedLesson(lesson.id(), lesson.teacherUserId(), lesson.type(), lesson.startsAt(),
                    lesson.endsAt(), lesson.capacity(), lesson.bookedCount(), lesson.status());
        }
    }
}
