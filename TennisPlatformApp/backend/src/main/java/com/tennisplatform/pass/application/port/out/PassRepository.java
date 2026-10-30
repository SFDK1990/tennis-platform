package com.tennisplatform.pass.application.port.out;

import com.tennisplatform.pass.domain.Pass;
import com.tennisplatform.pass.domain.PassLessonType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassRepository {

    Optional<Pass> findById(UUID id);

    /** {@code SELECT ... FOR UPDATE}: voiding waits for a booking that is spending the pass. */
    Optional<Pass> lockById(UUID id);

    /** All of the teacher's passes, or one student's when {@code studentUserId} is not null, newest first. */
    List<Pass> findByTeacher(UUID teacherUserId, UUID studentUserId);

    List<Pass> findByStudent(UUID studentUserId);

    /** The student's passes for one lesson type that are not voided. */
    List<Pass> findUsable(UUID teacherUserId, UUID studentUserId, PassLessonType lessonType);

    /** The same, locked with {@code SELECT ... FOR UPDATE} until the caller's transaction ends. */
    List<Pass> lockUsable(UUID teacherUserId, UUID studentUserId, PassLessonType lessonType);

    /** Not voided, paid from {@code from} included to {@code until} excluded. */
    List<Pass> findSold(UUID teacherUserId, LocalDate from, LocalDate until);

    Pass save(Pass pass);
}
