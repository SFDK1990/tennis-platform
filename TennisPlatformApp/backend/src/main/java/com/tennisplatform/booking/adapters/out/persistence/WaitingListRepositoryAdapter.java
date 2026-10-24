package com.tennisplatform.booking.adapters.out.persistence;

import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.domain.AlreadyWaitingException;
import com.tennisplatform.booking.domain.WaitingListEntry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
class WaitingListRepositoryAdapter implements WaitingListRepository {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String PAIR_CONSTRAINT = "ux_waiting_list_pair";

    private final WaitingListEntryJpaRepository jpa;

    WaitingListRepositoryAdapter(WaitingListEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public WaitingListEntry save(WaitingListEntry entry) {
        try {
            return jpa.saveAndFlush(WaitingListEntryEntity.fromDomain(entry)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() instanceof SQLException cause
                    && UNIQUE_VIOLATION.equals(cause.getSQLState())
                    && String.valueOf(cause.getMessage()).contains(PAIR_CONSTRAINT)) {
                throw new AlreadyWaitingException("You are already on this lesson's waiting list");
            }
            throw e;
        }
    }

    @Override
    public List<WaitingListEntry> findByLesson(UUID lessonId) {
        return jpa.findAllByLessonIdOrderByJoinedAtAscIdAsc(lessonId).stream()
                .map(WaitingListEntryEntity::toDomain)
                .toList();
    }

    @Override
    public List<WaitingListEntry> findByStudent(UUID studentUserId) {
        return jpa.findAllByStudentUserIdOrderByJoinedAtAsc(studentUserId).stream()
                .map(WaitingListEntryEntity::toDomain)
                .toList();
    }

    @Override
    public boolean exists(UUID lessonId, UUID studentUserId) {
        return jpa.existsByLessonIdAndStudentUserId(lessonId, studentUserId);
    }

    @Override
    public void delete(UUID lessonId, UUID studentUserId) {
        jpa.deleteEntry(lessonId, studentUserId);
    }

    @Override
    public void deleteAllOfLesson(UUID lessonId) {
        jpa.deleteAllOfLesson(lessonId);
    }

    @Override
    public void deleteAllOfStudentWithTeacher(UUID teacherUserId, UUID studentUserId) {
        jpa.deleteAllOfStudentWithTeacher(teacherUserId, studentUserId);
    }
}
