package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.domain.ObjectiveAchievement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_objectives")
@IdClass(StudentObjectiveEntity.Key.class)
class StudentObjectiveEntity {

    @Id
    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    @Id
    @Column(name = "objective_id", nullable = false, updatable = false)
    private UUID objectiveId;

    @Column(name = "achieved_on", nullable = false)
    private LocalDate achievedOn;

    protected StudentObjectiveEntity() {
    }

    static StudentObjectiveEntity fromDomain(ObjectiveAchievement achievement) {
        StudentObjectiveEntity entity = new StudentObjectiveEntity();
        entity.studentUserId = achievement.studentUserId();
        entity.objectiveId = achievement.objectiveId();
        entity.achievedOn = achievement.achievedOn();
        return entity;
    }

    ObjectiveAchievement toDomain() {
        return new ObjectiveAchievement(studentUserId, objectiveId, achievedOn);
    }

    record Key(UUID studentUserId, UUID objectiveId) implements Serializable {
    }
}
