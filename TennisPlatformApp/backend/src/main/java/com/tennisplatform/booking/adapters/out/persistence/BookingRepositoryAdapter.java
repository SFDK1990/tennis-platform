package com.tennisplatform.booking.adapters.out.persistence;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingAlreadyExistsException;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class BookingRepositoryAdapter implements BookingRepository {

    /** PostgreSQL SQLStates: standard codes, so they survive a change of wording or locale. */
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String EXCLUSION_VIOLATION = "23P01";

    /**
     * The constraint names, as v7-booking declares them. Matched in the message on top of the
     * SQLState because Hibernate does not report the name of an exclusion constraint - Fase 8
     * measured that - and so that a second unique index added one day is not silently reported as
     * a duplicate booking.
     */
    private static final String ACTIVE_PAIR_INDEX = "ux_bookings_active_pair";
    private static final String STUDENT_OVERLAP_CONSTRAINT = "ex_bookings_no_student_overlap";

    /** Newest lesson first, and within one lesson, in the order the seats were taken. */
    private static final Sort NEWEST_LESSON_FIRST =
            Sort.by(Sort.Order.desc("lessonStartsAt"), Sort.Order.asc("bookedAt"));

    private final BookingJpaRepository jpa;

    BookingRepositoryAdapter(BookingJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * {@code saveAndFlush} so a violation surfaces here, where it can be translated, and not at
     * commit, after the controller has already returned - the lesson learnt in Fase 8.
     */
    @Override
    public Booking save(Booking booking) {
        try {
            return jpa.saveAndFlush(entityFor(booking)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (violates(e, UNIQUE_VIOLATION, ACTIVE_PAIR_INDEX)) {
                throw new BookingAlreadyExistsException("You already have a seat in this lesson");
            }
            if (violates(e, EXCLUSION_VIOLATION, STUDENT_OVERLAP_CONSTRAINT)) {
                throw new StudentScheduleOverlapException("You already have a lesson at that time");
            }
            throw e;
        }
    }

    private BookingEntity entityFor(Booking booking) {
        if (booking.id() == null) {
            return BookingEntity.fromDomain(booking);
        }
        return jpa.findById(booking.id())
                .map(existing -> {
                    existing.apply(booking);
                    return existing;
                })
                .orElseGet(() -> BookingEntity.fromDomain(booking));
    }

    private static boolean violates(DataIntegrityViolationException e, String sqlState, String name) {
        return e.getMostSpecificCause() instanceof SQLException cause
                && sqlState.equals(cause.getSQLState())
                && String.valueOf(cause.getMessage()).contains(name);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jpa.findById(id).map(BookingEntity::toDomain);
    }

    @Override
    public List<Booking> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(BookingEntity::toDomain).toList();
    }

    @Override
    public boolean existsConfirmed(UUID lessonId, UUID studentUserId) {
        return jpa.existsConfirmed(lessonId, studentUserId);
    }

    @Override
    public boolean existsOverlappingConfirmed(UUID studentUserId, Instant startsAt, Instant endsAt) {
        return jpa.existsOverlappingConfirmed(studentUserId, startsAt, endsAt);
    }

    @Override
    public Map<UUID, Integer> countConfirmed(Collection<UUID> lessonIds) {
        return jpa.countConfirmed(lessonIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Long) row[1]).intValue()));
    }

    @Override
    public void cancelConfirmedOfLesson(UUID lessonId, BookingStatus as, Instant at) {
        jpa.cancelConfirmedOfLesson(lessonId, as, at);
    }

    @Override
    public void cancelUpcoming(UUID teacherUserId, UUID studentUserId, BookingStatus as, Instant now) {
        jpa.cancelUpcoming(teacherUserId, studentUserId, as, now);
    }

    @Override
    public Slice findForStudent(UUID studentUserId, BookingStatus status, int page, int size) {
        return slice(equal("studentUserId", studentUserId).and(equal("status", status)), page, size);
    }

    @Override
    public Slice findForTeacher(UUID teacherUserId, UUID lessonId, BookingStatus status, int page,
                                int size) {
        return slice(equal("teacherUserId", teacherUserId)
                .and(equal("lessonId", lessonId))
                .and(equal("status", status)), page, size);
    }

    private Slice slice(Specification<BookingEntity> where, int page, int size) {
        Page<BookingEntity> found = jpa.findAll(where, PageRequest.of(page, size, NEWEST_LESSON_FIRST));
        return new Slice(found.map(BookingEntity::toDomain).getContent(), found.getTotalElements());
    }

    /**
     * An optional filter: {@code null} means "do not filter". Built as a specification rather than
     * as {@code :param is null or ...} in JPQL, because PostgreSQL cannot infer the type of a null
     * UUID parameter and fails the query instead of ignoring the condition.
     */
    private static Specification<BookingEntity> equal(String field, Object value) {
        return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }
}
