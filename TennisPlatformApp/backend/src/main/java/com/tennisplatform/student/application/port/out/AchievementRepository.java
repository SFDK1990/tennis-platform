package com.tennisplatform.student.application.port.out;

import com.tennisplatform.student.domain.ObjectiveAchievement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AchievementRepository {

    List<ObjectiveAchievement> findByStudent(UUID studentUserId);

    List<ObjectiveAchievement> findByStudents(Collection<UUID> studentUserIds);

    Optional<ObjectiveAchievement> find(UUID studentUserId, UUID objectiveId);

    /** Inserts it, or changes the day of the one already there. */
    void save(ObjectiveAchievement achievement);

    void delete(UUID studentUserId, UUID objectiveId);

    void deleteAllOf(UUID studentUserId);
}
