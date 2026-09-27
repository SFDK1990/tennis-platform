package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.MarkAttendance;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.Attendance;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingNotFoundException;
import com.tennisplatform.booking.domain.InvalidBookingRequestException;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MarkAttendanceService implements MarkAttendance {

    private static final Logger log = LoggerFactory.getLogger(MarkAttendanceService.class);

    private final BookingRepository bookings;
    private final BookingViews views;
    private final Clock clock;

    public MarkAttendanceService(BookingRepository bookings, GetLesson getLesson, Clock clock) {
        this.bookings = bookings;
        this.views = new BookingViews(getLesson);
        this.clock = clock;
    }

    /**
     * Every entry is validated before anything is written, so a batch with one bad entry leaves
     * the lesson exactly as it was. The transaction would roll back anyway; validating first means
     * not relying on that for correctness.
     */
    @Override
    @Transactional
    public List<BookingView> mark(UUID teacherUserId, UUID lessonId, List<Entry> entries) {
        long distinct = entries.stream().map(Entry::bookingId).distinct().count();
        if (distinct != entries.size()) {
            throw new InvalidBookingRequestException("Each booking can appear only once in a batch");
        }

        Map<UUID, Booking> found = bookings.findAllById(entries.stream().map(Entry::bookingId).toList())
                .stream()
                .collect(Collectors.toMap(Booking::id, Function.identity()));

        Instant now = clock.instant();
        List<Booking> marked = new ArrayList<>();
        for (Entry entry : entries) {
            Booking booking = found.get(entry.bookingId());
            // Not in this lesson, or not this teacher's: the same 404 as a booking that does not
            // exist, for the reason CancelBookingService gives.
            if (booking == null
                    || !booking.lessonId().equals(lessonId)
                    || !booking.teacherUserId().equals(teacherUserId)) {
                throw new BookingNotFoundException(
                        "No booking with id " + entry.bookingId() + " in this lesson");
            }
            marked.add(booking.markAttendance(Attendance.markable(entry.attendance()), now));
        }

        log.info("Attendance marked for {} bookings of lesson {}", marked.size(), lessonId);
        return views.of(marked.stream().map(bookings::save).toList());
    }
}
