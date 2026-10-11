package com.tennisplatform.booking.application.port.out;

import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    /**
     * Saves a new booking or an updated one.
     *
     * <p>Translates the two constraints that guard against races into the same answers the
     * application's own checks give: the unique index into {@code BookingAlreadyExistsException}
     * and the exclusion constraint into {@code StudentScheduleOverlapException}.
     */
    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    List<Booking> findAllById(Collection<UUID> ids);

    boolean existsConfirmed(UUID lessonId, UUID studentUserId);

    List<Booking> findByStudentInLessons(UUID studentUserId, Collection<UUID> lessonIds);

    /** Whether the student already has a confirmed booking that overlaps the interval. */
    boolean existsOverlappingConfirmed(UUID studentUserId, Instant startsAt, Instant endsAt);

    /** Confirmed bookings per lesson; lessons with none are absent from the map. */
    Map<UUID, Integer> countConfirmed(Collection<UUID> lessonIds);

    /** Cancels every confirmed booking of the lesson with the given status, in one statement. */
    void cancelConfirmedOfLesson(UUID lessonId, BookingStatus as, Instant at);

    /** Cancels the student's confirmed bookings with this teacher whose lesson starts after {@code now}. */
    void cancelUpcoming(UUID teacherUserId, UUID studentUserId, BookingStatus as, Instant now);

    Slice findForStudent(UUID studentUserId, BookingStatus status, int page, int size);

    /** Every booking, whoever it belongs to: only the administration reads this. */
    Slice findAll(UUID lessonId, BookingStatus status, int page, int size);

    Slice findForTeacher(UUID teacherUserId, UUID lessonId, BookingStatus status, int page, int size);

    /** One page of bookings and the total they were cut from. */
    record Slice(List<Booking> items, long total) {

        public Slice {
            items = List.copyOf(items);
        }
    }
}
