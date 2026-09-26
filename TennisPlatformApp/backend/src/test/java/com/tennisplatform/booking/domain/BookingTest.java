package com.tennisplatform.booking.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final Instant STARTS = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-06-01T10:00:00Z");

    private static Booking confirmed() {
        return Booking.confirm(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), STARTS, ENDS,
                STARTS.minus(Duration.ofDays(7)));
    }

    @Test
    void startsConfirmedWithAttendancePending() {
        Booking booking = confirmed();

        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.attendance()).isEqualTo(Attendance.PENDING);
        assertThat(booking.cancelledAt()).isNull();
    }

    /** "Up to 24 hours before": the boundary itself is still in time. */
    @Test
    void aStudentMayCancelWithExactlyTwentyFourHoursOfNotice() {
        Instant now = STARTS.minus(Booking.CANCELLATION_WINDOW);

        Booking cancelled = confirmed().cancelByStudent(now);

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED_BY_STUDENT);
        assertThat(cancelled.cancelledAt()).isEqualTo(now);
    }

    @Test
    void aStudentMayNotCancelOneSecondInsideTheWindow() {
        Instant now = STARTS.minus(Booking.CANCELLATION_WINDOW).plusSeconds(1);

        assertThatThrownBy(() -> confirmed().cancelByStudent(now))
                .isInstanceOf(CancellationWindowExpiredException.class);
    }

    /** The window protects the teacher, so it binds neither the teacher nor an admin. */
    @Test
    void theTeacherAndAnAdminMayCancelInsideTheWindow() {
        Instant anHourBefore = STARTS.minus(Duration.ofHours(1));

        assertThat(confirmed().cancelByTeacher(anHourBefore).status())
                .isEqualTo(BookingStatus.CANCELLED_BY_TEACHER);
        assertThat(confirmed().cancelByAdmin(anHourBefore).status())
                .isEqualTo(BookingStatus.CANCELLED_BY_ADMIN);
    }

    @Test
    void nobodyCancelsABookingOnceTheLessonHasStarted() {
        assertThatThrownBy(() -> confirmed().cancelByTeacher(STARTS))
                .isInstanceOf(LessonAlreadyStartedException.class);
        assertThatThrownBy(() -> confirmed().cancelByAdmin(STARTS.plusSeconds(60)))
                .isInstanceOf(LessonAlreadyStartedException.class);
    }

    @Test
    void aCancelledBookingCannotBeCancelledAgain() {
        Booking cancelled = confirmed().cancelByTeacher(STARTS.minus(Duration.ofDays(2)));

        assertThatThrownBy(() -> cancelled.cancelByAdmin(STARTS.minus(Duration.ofDays(1))))
                .isInstanceOf(BookingAlreadyCancelledException.class);
    }

    @Test
    void attendanceCannotBeRecordedBeforeTheLessonStarts() {
        assertThatThrownBy(() -> confirmed().markAttendance(Attendance.ATTENDED, STARTS.minusSeconds(1)))
                .isInstanceOf(AttendanceNotYetOpenException.class);
    }

    /** Ticking the wrong box has to be undoable, so a second marking replaces the first. */
    @Test
    void attendanceCanBeRecordedFromTheStartAndCorrectedAfterwards() {
        Booking attended = confirmed().markAttendance(Attendance.ATTENDED, STARTS);
        Booking corrected = attended.markAttendance(Attendance.NO_SHOW, ENDS.plus(Duration.ofDays(1)));

        assertThat(attended.attendance()).isEqualTo(Attendance.ATTENDED);
        assertThat(corrected.attendance()).isEqualTo(Attendance.NO_SHOW);
        assertThat(corrected.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void attendanceIsNotRecordedForACancelledBooking() {
        Booking cancelled = confirmed().cancelByTeacher(STARTS.minus(Duration.ofDays(2)));

        assertThatThrownBy(() -> cancelled.markAttendance(Attendance.ATTENDED, ENDS))
                .isInstanceOf(BookingAlreadyCancelledException.class);
    }

    @Test
    void onlyAttendedAndNoShowCanBeMarked() {
        assertThat(Attendance.markable(" no_show ")).isEqualTo(Attendance.NO_SHOW);
        assertThatThrownBy(() -> Attendance.markable("PENDING"))
                .isInstanceOf(InvalidBookingRequestException.class);
        assertThatThrownBy(() -> Attendance.markable(null))
                .isInstanceOf(InvalidBookingRequestException.class);
    }

    @Test
    void anAbsentStatusFilterMeansAnyAndAnUnknownOneIsRefused() {
        assertThat(BookingStatus.filter(null)).isNull();
        assertThat(BookingStatus.filter("confirmed")).isEqualTo(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> BookingStatus.filter("ATTENDED"))
                .isInstanceOf(InvalidBookingRequestException.class);
    }
}
