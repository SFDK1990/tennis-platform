package com.tennisplatform.booking.application.port.out;

import com.tennisplatform.booking.domain.WaitingListEntry;

import java.util.List;
import java.util.UUID;

public interface WaitingListRepository {

    /** Translates the unique pair into {@code AlreadyWaitingException}, for two joins that race. */
    WaitingListEntry save(WaitingListEntry entry);

    /** The lesson's list in order: first come, first served. */
    List<WaitingListEntry> findByLesson(UUID lessonId);

    List<WaitingListEntry> findByStudent(UUID studentUserId);

    boolean exists(UUID lessonId, UUID studentUserId);

    void delete(UUID lessonId, UUID studentUserId);

    void deleteAllOfLesson(UUID lessonId);

    /** Whatever the student waits for with this teacher, when the two part ways. */
    void deleteAllOfStudentWithTeacher(UUID teacherUserId, UUID studentUserId);
}
