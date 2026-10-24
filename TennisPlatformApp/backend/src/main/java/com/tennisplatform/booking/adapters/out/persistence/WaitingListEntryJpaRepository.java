package com.tennisplatform.booking.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface WaitingListEntryJpaRepository extends JpaRepository<WaitingListEntryEntity, UUID> {

    List<WaitingListEntryEntity> findAllByLessonIdOrderByJoinedAtAscIdAsc(UUID lessonId);

    List<WaitingListEntryEntity> findAllByStudentUserIdOrderByJoinedAtAsc(UUID studentUserId);

    boolean existsByLessonIdAndStudentUserId(UUID lessonId, UUID studentUserId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WaitingListEntryEntity e where e.lessonId = :lessonId and e.studentUserId = :studentUserId")
    int deleteEntry(@Param("lessonId") UUID lessonId, @Param("studentUserId") UUID studentUserId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WaitingListEntryEntity e where e.lessonId = :lessonId")
    int deleteAllOfLesson(@Param("lessonId") UUID lessonId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from WaitingListEntryEntity e
            where e.teacherUserId = :teacherUserId and e.studentUserId = :studentUserId
            """)
    int deleteAllOfStudentWithTeacher(@Param("teacherUserId") UUID teacherUserId,
                                      @Param("studentUserId") UUID studentUserId);
}
