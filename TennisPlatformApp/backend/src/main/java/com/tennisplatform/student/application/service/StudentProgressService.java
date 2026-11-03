package com.tennisplatform.student.application.service;

import com.tennisplatform.student.application.port.in.ObjectiveAchieved;
import com.tennisplatform.student.application.port.in.StudentProgress;
import com.tennisplatform.student.application.port.in.StudentProgressView;
import com.tennisplatform.student.application.port.in.StudentProgressView.LevelProgress;
import com.tennisplatform.student.application.port.in.StudentProgressView.ObjectiveProgress;
import com.tennisplatform.student.application.port.out.AchievementRepository;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.domain.AchievedOnInFutureException;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.ObjectiveAchievement;
import com.tennisplatform.student.domain.StudentNotManagedException;
import com.tennisplatform.student.domain.UnknownObjectiveException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ObjectiveView;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The objectives each student has achieved (44-fase29-analisis-progreso.md).
 *
 * <p>The objectives belong to {@code teacher}, the marks to this module, and they meet here in
 * memory: one teacher has a handful of levels with a handful of objectives each.
 */
public class StudentProgressService implements StudentProgress {

    private static final Logger log = LoggerFactory.getLogger(StudentProgressService.class);

    private final ManagedStudentRepository relationships;
    private final AchievementRepository achievements;
    private final QueryLevels levels;
    private final GetTeacherProfile teachers;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public StudentProgressService(ManagedStudentRepository relationships, AchievementRepository achievements,
                                  QueryLevels levels, GetTeacherProfile teachers, ApplicationEventPublisher events,
                                  Clock clock) {
        this.relationships = relationships;
        this.achievements = achievements;
        this.levels = levels;
        this.teachers = teachers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProgressView ofManagedStudent(UUID teacherUserId, UUID studentUserId) {
        ManagedStudent relationship = relationships.findByPair(teacherUserId, studentUserId)
                .orElseThrow(() -> new StudentNotManagedException("This student is not managed by this teacher"));
        return progress(teacherUserId, relationship.levelId(), achievements.findByStudent(studentUserId), true);
    }

    @Override
    @Transactional
    public StudentProgressView mark(UUID teacherUserId, UUID studentUserId, UUID objectiveId, LocalDate achievedOn) {
        managed(teacherUserId, studentUserId);
        levels.liveObjective(objectiveId, teacherUserId)
                .orElseThrow(() -> new UnknownObjectiveException("No objective with id " + objectiveId));
        LocalDate today = LocalDate.now(clock.withZone(zoneOf(teacherUserId)));
        LocalDate day = achievedOn == null ? today : achievedOn;
        if (day.isAfter(today)) {
            throw new AchievedOnInFutureException("An objective cannot be achieved after today");
        }
        boolean first = achievements.find(studentUserId, objectiveId).isEmpty();
        achievements.save(new ObjectiveAchievement(studentUserId, objectiveId, day));
        if (first) {
            events.publishEvent(new ObjectiveAchieved(teacherUserId, studentUserId, objectiveId));
        }
        log.info("Objective {} marked as achieved for student {}", objectiveId, studentUserId);
        return ofManagedStudent(teacherUserId, studentUserId);
    }

    @Override
    @Transactional
    public void unmark(UUID teacherUserId, UUID studentUserId, UUID objectiveId) {
        managed(teacherUserId, studentUserId);
        boolean ours = levels.allObjectivesOf(teacherUserId).stream().anyMatch(o -> o.id().equals(objectiveId));
        if (!ours) {
            throw new UnknownObjectiveException("No objective with id " + objectiveId);
        }
        achievements.delete(studentUserId, objectiveId);
        log.info("Objective {} unmarked for student {}", objectiveId, studentUserId);
    }

    /** With a single teacher, the student has one or none; with none, there is nothing to show. */
    @Override
    @Transactional(readOnly = true)
    public StudentProgressView ofStudent(UUID studentUserId) {
        return relationships.findManagingTeachers(studentUserId).stream()
                .findFirst()
                .flatMap(teacherUserId -> relationships.findByPair(teacherUserId, studentUserId))
                .map(relationship -> progress(relationship.teacherUserId(), relationship.levelId(),
                        achievements.findByStudent(studentUserId), false))
                .orElseGet(() -> new StudentProgressView(List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> withLevelCompleted(UUID teacherUserId, Collection<UUID> studentUserIds) {
        if (studentUserIds.isEmpty()) {
            return Set.of();
        }
        Map<UUID, UUID> levelOf = relationships.findAllByTeacher(teacherUserId).stream()
                .filter(relationship -> studentUserIds.contains(relationship.studentUserId()))
                .filter(relationship -> relationship.levelId() != null)
                .collect(Collectors.toMap(ManagedStudent::studentUserId, ManagedStudent::levelId));
        if (levelOf.isEmpty()) {
            return Set.of();
        }
        Map<UUID, List<ObjectiveView>> live = levels.levelsOf(teacherUserId).stream()
                .collect(Collectors.toMap(LevelView::id, LevelView::objectives));
        Map<UUID, Set<UUID>> achievedBy = achievements.findByStudents(levelOf.keySet()).stream()
                .collect(Collectors.groupingBy(ObjectiveAchievement::studentUserId,
                        Collectors.mapping(ObjectiveAchievement::objectiveId, Collectors.toSet())));
        Set<UUID> completed = new HashSet<>();
        levelOf.forEach((student, level) -> {
            if (allAchieved(live.getOrDefault(level, List.of()), achievedBy.getOrDefault(student, Set.of()))) {
                completed.add(student);
            }
        });
        return completed;
    }

    /**
     * Each level with its live objectives, then the retired ones the student achieved. The student
     * only sees their current level and those where they achieved something (decision 3).
     */
    private StudentProgressView progress(UUID teacherUserId, UUID currentLevelId, List<ObjectiveAchievement> marks,
                                         boolean everyLevel) {
        Map<UUID, LocalDate> achieved = marks.stream()
                .collect(Collectors.toMap(ObjectiveAchievement::objectiveId, ObjectiveAchievement::achievedOn));
        Map<UUID, List<ObjectiveView>> retiredByLevel = levels.allObjectivesOf(teacherUserId).stream()
                .filter(ObjectiveView::retired)
                .filter(objective -> achieved.containsKey(objective.id()))
                .collect(Collectors.groupingBy(ObjectiveView::levelId));
        List<LevelProgress> result = new ArrayList<>();
        for (LevelView level : levels.levelsOf(teacherUserId)) {
            List<ObjectiveView> shown = new ArrayList<>(level.objectives());
            shown.addAll(retiredByLevel.getOrDefault(level.id(), List.of()));
            boolean current = level.id().equals(currentLevelId);
            boolean anyAchieved = shown.stream().anyMatch(objective -> achieved.containsKey(objective.id()));
            if (everyLevel || current || anyAchieved) {
                result.add(new LevelProgress(level.id(), level.name(), current,
                        allAchieved(level.objectives(), achieved.keySet()),
                        shown.stream()
                                .map(objective -> new ObjectiveProgress(objective.id(), objective.title(),
                                        achieved.get(objective.id())))
                                .toList()));
            }
        }
        return new StudentProgressView(result);
    }

    private static boolean allAchieved(List<ObjectiveView> live, Set<UUID> achieved) {
        return !live.isEmpty() && live.stream().map(ObjectiveView::id).allMatch(achieved::contains);
    }

    private void managed(UUID teacherUserId, UUID studentUserId) {
        relationships.findByPair(teacherUserId, studentUserId)
                .filter(ManagedStudent::isManaged)
                .orElseThrow(() -> new StudentNotManagedException("This student is not managed by this teacher"));
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        return ZoneId.of(teachers.byUserId(teacherUserId).map(TeacherProfileView::timezone).orElseThrow());
    }
}
