package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.application.port.out.PassRepository;
import com.tennisplatform.pass.domain.Pass;
import com.tennisplatform.pass.domain.PassLessonType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PassRepositoryAdapter implements PassRepository {

    private final PassJpaRepository jpa;

    PassRepositoryAdapter(PassJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Pass> findById(UUID id) {
        return jpa.findById(id).map(PassEntity::toDomain);
    }

    @Override
    public Optional<Pass> lockById(UUID id) {
        return jpa.findByIdForUpdate(id).map(PassEntity::toDomain);
    }

    @Override
    public List<Pass> findByTeacher(UUID teacherUserId, UUID studentUserId) {
        List<PassEntity> found = studentUserId == null
                ? jpa.findAllByTeacherUserIdOrderByCreatedAtDesc(teacherUserId)
                : jpa.findAllByTeacherUserIdAndStudentUserIdOrderByCreatedAtDesc(teacherUserId, studentUserId);
        return found.stream().map(PassEntity::toDomain).toList();
    }

    @Override
    public List<Pass> findByStudent(UUID studentUserId) {
        return jpa.findAllByStudentUserIdOrderByCreatedAtDesc(studentUserId).stream().map(PassEntity::toDomain).toList();
    }

    @Override
    public List<Pass> findUsable(UUID teacherUserId, UUID studentUserId, PassLessonType lessonType) {
        return jpa.findUsable(teacherUserId, studentUserId, lessonType).stream().map(PassEntity::toDomain).toList();
    }

    @Override
    public List<Pass> lockUsable(UUID teacherUserId, UUID studentUserId, PassLessonType lessonType) {
        return jpa.lockUsable(teacherUserId, studentUserId, lessonType).stream().map(PassEntity::toDomain).toList();
    }

    @Override
    public List<Pass> findSold(UUID teacherUserId, LocalDate from, LocalDate until) {
        return jpa.findSold(teacherUserId, from, until).stream().map(PassEntity::toDomain).toList();
    }

    @Override
    public Pass save(Pass pass) {
        return jpa.saveAndFlush(PassEntity.fromDomain(pass)).toDomain();
    }
}
