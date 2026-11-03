package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.application.port.out.AchievementRepository;
import com.tennisplatform.student.domain.ObjectiveAchievement;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class AchievementRepositoryAdapter implements AchievementRepository {

    private final StudentObjectiveJpaRepository jpa;

    AchievementRepositoryAdapter(StudentObjectiveJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ObjectiveAchievement> findByStudent(UUID studentUserId) {
        return jpa.findAllByStudentUserId(studentUserId).stream().map(StudentObjectiveEntity::toDomain).toList();
    }

    @Override
    public List<ObjectiveAchievement> findByStudents(Collection<UUID> studentUserIds) {
        if (studentUserIds.isEmpty()) {
            return List.of();
        }
        return jpa.findAllByStudentUserIdIn(studentUserIds).stream().map(StudentObjectiveEntity::toDomain).toList();
    }

    @Override
    public Optional<ObjectiveAchievement> find(UUID studentUserId, UUID objectiveId) {
        return jpa.findById(new StudentObjectiveEntity.Key(studentUserId, objectiveId))
                .map(StudentObjectiveEntity::toDomain);
    }

    @Override
    public void save(ObjectiveAchievement achievement) {
        jpa.saveAndFlush(StudentObjectiveEntity.fromDomain(achievement));
    }

    @Override
    public void delete(UUID studentUserId, UUID objectiveId) {
        jpa.deleteById(new StudentObjectiveEntity.Key(studentUserId, objectiveId));
        jpa.flush();
    }

    @Override
    public void deleteAllOf(UUID studentUserId) {
        jpa.deleteAllOfStudent(studentUserId);
    }
}
