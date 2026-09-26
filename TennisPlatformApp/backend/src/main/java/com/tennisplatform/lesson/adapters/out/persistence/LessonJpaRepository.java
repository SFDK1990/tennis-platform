package com.tennisplatform.lesson.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface LessonJpaRepository extends JpaRepository<LessonEntity, UUID> {

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
