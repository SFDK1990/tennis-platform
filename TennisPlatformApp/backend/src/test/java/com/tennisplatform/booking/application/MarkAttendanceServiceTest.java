package com.tennisplatform.booking.application;

import com.tennisplatform.booking.application.port.in.MarkAttendance.Entry;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.service.MarkAttendanceService;
import com.tennisplatform.booking.domain.Attendance;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingNotFoundException;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.booking.domain.InvalidBookingRequestException;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkAttendanceServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID LESSON_ID = UUID.randomUUID();
    private static final Instant STARTS = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-06-01T10:00:00Z");

    private BookingRepository bookings;
    private MarkAttendanceService service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        GetLesson getLesson = mock(GetLesson.class);
        when(getLesson.byIds(anyCollection())).thenReturn(List.of());
        when(bookings.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new MarkAttendanceService(bookings, getLesson, Clock.fixed(ENDS, ZoneOffset.UTC));
    }

    @Test
    void recordsWhoCameAndWhoDidNot() {
        Booking came = booking(LESSON_ID);
        Booking missed = booking(LESSON_ID);
        when(bookings.findAllById(any())).thenReturn(List.of(came, missed));

        service.mark(TEACHER_ID, LESSON_ID,
                List.of(new Entry(came.id(), "ATTENDED"), new Entry(missed.id(), "NO_SHOW")));

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Booking::attendance)
                .containsExactly(Attendance.ATTENDED, Attendance.NO_SHOW);
    }

    /** All or nothing: one entry from another lesson and not a single one is written. */
    @Test
    void appliesNothingWhenOneEntryBelongsToAnotherLesson() {
        Booking mine = booking(LESSON_ID);
        Booking elsewhere = booking(UUID.randomUUID());
        when(bookings.findAllById(any())).thenReturn(List.of(mine, elsewhere));

        assertThatThrownBy(() -> service.mark(TEACHER_ID, LESSON_ID,
                List.of(new Entry(mine.id(), "ATTENDED"), new Entry(elsewhere.id(), "ATTENDED"))))
                .isInstanceOf(BookingNotFoundException.class);

        verify(bookings, never()).save(any());
    }

    @Test
    void refusesTheSameBookingTwiceInOneBatch() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.mark(TEACHER_ID, LESSON_ID,
                List.of(new Entry(id, "ATTENDED"), new Entry(id, "NO_SHOW"))))
                .isInstanceOf(InvalidBookingRequestException.class);
    }

    private static Booking booking(UUID lessonId) {
        return Booking.rehydrate(UUID.randomUUID(), lessonId, TEACHER_ID, UUID.randomUUID(),
                BookingStatus.CONFIRMED, Attendance.PENDING, STARTS, ENDS,
                STARTS.minusSeconds(86_400), null);
    }
}
