package com.tennisplatform.booking.adapters.out.persistence;

import com.tennisplatform.booking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID>,
        JpaSpecificationExecutor<BookingEntity> {

    @Query("""
            select count(b) > 0 from BookingEntity b
            where b.lessonId = :lessonId
              and b.studentUserId = :studentUserId
              and b.status = com.tennisplatform.booking.domain.BookingStatus.CONFIRMED
            """)
    boolean existsConfirmed(@Param("lessonId") UUID lessonId,
                            @Param("studentUserId") UUID studentUserId);

    /** Same condition as the exclusion constraint: confirmed only, half-open intervals. */
    @Query("""
            select count(b) > 0 from BookingEntity b
            where b.studentUserId = :studentUserId
              and b.status = com.tennisplatform.booking.domain.BookingStatus.CONFIRMED
              and b.lessonStartsAt < :endsAt
              and :startsAt < b.lessonEndsAt
            """)
    boolean existsOverlappingConfirmed(@Param("studentUserId") UUID studentUserId,
                                       @Param("startsAt") Instant startsAt,
                                       @Param("endsAt") Instant endsAt);

    @Query("""
            select b.lessonId, count(b) from BookingEntity b
            where b.lessonId in :lessonIds
              and b.status = com.tennisplatform.booking.domain.BookingStatus.CONFIRMED
            group by b.lessonId
            """)
    List<Object[]> countConfirmed(@Param("lessonIds") Collection<UUID> lessonIds);

    /**
     * Bulk updates, one statement each. They bypass the persistence context, so {@code
     * clearAutomatically} keeps an entity loaded earlier from reporting its old status, and
     * {@code updatedAt} is set by hand because {@code @UpdateTimestamp} never sees them.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BookingEntity b
            set b.status = :newStatus, b.cancelledAt = :at, b.updatedAt = :at
            where b.lessonId = :lessonId
              and b.status = com.tennisplatform.booking.domain.BookingStatus.CONFIRMED
            """)
    int cancelConfirmedOfLesson(@Param("lessonId") UUID lessonId, @Param("newStatus") BookingStatus newStatus,
                                @Param("at") Instant at);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BookingEntity b
            set b.status = :newStatus, b.cancelledAt = :now, b.updatedAt = :now
            where b.teacherUserId = :teacherUserId
              and b.studentUserId = :studentUserId
              and b.status = com.tennisplatform.booking.domain.BookingStatus.CONFIRMED
              and b.lessonStartsAt > :now
            """)
    int cancelUpcoming(@Param("teacherUserId") UUID teacherUserId,
                       @Param("studentUserId") UUID studentUserId, @Param("newStatus") BookingStatus newStatus,
                       @Param("now") Instant now);
}
