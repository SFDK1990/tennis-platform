package com.tennisplatform.lesson.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LessonJpaRepository extends JpaRepository<LessonEntity, UUID> {

    /**
     * {@code SELECT ... FOR UPDATE}. A pessimistic lock rather than an optimistic one because
     * the contended case is the normal one here - two students after the last seat - and an
     * optimistic lock would turn every such race into a failed write that has to be retried or
     * translated, instead of simply making the second request wait its turn.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LessonEntity l where l.id = :id")
    Optional<LessonEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Everything that touches the interval, cancelled lessons included: the listing shows what
     * was cancelled too, because a teacher looking at last Tuesday wants to know why it is empty.
     */
    @Query("""
            select l from LessonEntity l
            where l.teacherUserId = :teacherUserId
              and l.startsAt < :end
              and :start < l.endsAt
            order by l.startsAt asc
            """)
    List<LessonEntity> findTouching(@Param("teacherUserId") UUID teacherUserId,
                                    @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Cancelled lessons do not count, which is what lets the teacher reuse the slot they just
     * freed - the exclusion constraint in the schema filters on exactly the same condition.
     */
    @Query("""
            select count(l) > 0 from LessonEntity l
            where l.teacherUserId = :teacherUserId
              and l.cancelledAt is null
              and l.startsAt < :endsAt
              and :startsAt < l.endsAt
            """)
    boolean existsOverlapping(@Param("teacherUserId") UUID teacherUserId,
                              @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);
}
