package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.domain.PassLessonType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PassJpaRepository extends JpaRepository<PassEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PassEntity p where p.id = :id")
    Optional<PassEntity> findByIdForUpdate(@Param("id") UUID id);

    List<PassEntity> findAllByTeacherUserIdOrderByCreatedAtDesc(UUID teacherUserId);

    List<PassEntity> findAllByTeacherUserIdAndStudentUserIdOrderByCreatedAtDesc(UUID teacherUserId,
                                                                                 UUID studentUserId);

    List<PassEntity> findAllByStudentUserIdOrderByCreatedAtDesc(UUID studentUserId);

    @Query("""
            select p from PassEntity p
            where p.teacherUserId = :teacherUserId and p.studentUserId = :studentUserId
              and p.lessonType = :lessonType and p.voidedAt is null
            order by p.id
            """)
    List<PassEntity> findUsable(@Param("teacherUserId") UUID teacherUserId,
                                @Param("studentUserId") UUID studentUserId,
                                @Param("lessonType") PassLessonType lessonType);

    /**
     * Every pass of the student for that kind of lesson, not only the one that will pay: which
     * one pays depends on how many lessons each has left, and that is what the lock protects.
     * Ordered by id so two transactions always take the rows in the same order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from PassEntity p
            where p.teacherUserId = :teacherUserId and p.studentUserId = :studentUserId
              and p.lessonType = :lessonType and p.voidedAt is null
            order by p.id
            """)
    List<PassEntity> lockUsable(@Param("teacherUserId") UUID teacherUserId,
                                @Param("studentUserId") UUID studentUserId,
                                @Param("lessonType") PassLessonType lessonType);

    @Query("""
            select p from PassEntity p
            where p.teacherUserId = :teacherUserId and p.voidedAt is null
              and p.paidOn >= :from and p.paidOn < :until
            """)
    List<PassEntity> findSold(@Param("teacherUserId") UUID teacherUserId, @Param("from") LocalDate from,
                              @Param("until") LocalDate until);
}
