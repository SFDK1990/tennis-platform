package com.tennisplatform.teacher.application.service;

import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ManageLevels;
import com.tennisplatform.teacher.application.port.in.ObjectiveView;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import com.tennisplatform.teacher.application.port.out.LevelRepository;
import com.tennisplatform.teacher.application.port.out.ObjectiveRepository;
import com.tennisplatform.teacher.domain.Level;
import com.tennisplatform.teacher.domain.LevelNotFoundException;
import com.tennisplatform.teacher.domain.LevelObjective;
import com.tennisplatform.teacher.domain.ObjectiveNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LevelsService implements ManageLevels, QueryLevels {

    private static final Logger log = LoggerFactory.getLogger(LevelsService.class);

    private final LevelRepository levels;
    private final ObjectiveRepository objectives;
    private final GetTeacherProfile teacherProfile;
    private final Clock clock;

    public LevelsService(LevelRepository levels, ObjectiveRepository objectives, GetTeacherProfile teacherProfile,
                         Clock clock) {
        this.levels = levels;
        this.objectives = objectives;
        this.teacherProfile = teacherProfile;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LevelView> ofTheTeacher() {
        return levelsOf(teacherProfile.get().userId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LevelView> levelsOf(UUID teacherUserId) {
        List<Level> found = levels.findByTeacher(teacherUserId);
        Map<UUID, List<ObjectiveView>> live = objectives.findByLevels(found.stream().map(Level::id).toList()).stream()
                .filter(objective -> !objective.isRetired())
                .map(ObjectiveView::from)
                .collect(Collectors.groupingBy(ObjectiveView::levelId));
        return found.stream().map(level -> LevelView.from(level, live.getOrDefault(level.id(), List.of()))).toList();
    }

    @Override
    @Transactional
    public LevelView create(UUID teacherUserId, String name) {
        List<Level> existing = levels.findByTeacher(teacherUserId);
        Level saved = levels.save(new Level(null, teacherUserId, name, existing.size() + 1));
        log.info("Level {} created", saved.id());
        return LevelView.from(saved, List.of());
    }

    @Override
    @Transactional
    public LevelView update(UUID teacherUserId, UUID levelId, String name, Integer position) {
        Level level = owned(teacherUserId, levelId);
        if (name != null) {
            level = levels.save(level.renamed(name));
        }
        if (position != null) {
            level = moveTo(teacherUserId, level, position);
        }
        return LevelView.from(level, liveOf(level.id()).stream().map(ObjectiveView::from).toList());
    }

    @Override
    @Transactional
    public void delete(UUID teacherUserId, UUID levelId) {
        owned(teacherUserId, levelId);
        levels.delete(levelId);
        renumber(levels.findByTeacher(teacherUserId));
        log.info("Level {} deleted", levelId);
    }

    @Override
    @Transactional
    public ObjectiveView addObjective(UUID teacherUserId, UUID levelId, String title) {
        owned(teacherUserId, levelId);
        LevelObjective saved = objectives.save(new LevelObjective(null, levelId, title, liveOf(levelId).size() + 1,
                null));
        log.info("Objective {} added to level {}", saved.id(), levelId);
        return ObjectiveView.from(saved);
    }

    @Override
    @Transactional
    public ObjectiveView updateObjective(UUID teacherUserId, UUID objectiveId, String title, Integer position) {
        LevelObjective objective = ownedObjective(teacherUserId, objectiveId);
        if (title != null) {
            objective = objectives.save(objective.renamed(title));
        }
        if (position != null) {
            objective = moveTo(objective, position);
        }
        return ObjectiveView.from(objective);
    }

    @Override
    @Transactional
    public void retireObjective(UUID teacherUserId, UUID objectiveId) {
        LevelObjective objective = ownedObjective(teacherUserId, objectiveId);
        objectives.save(objective.retired(clock.instant()));
        renumberObjectives(liveOf(objective.levelId()));
        log.info("Objective {} retired", objectiveId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectiveView> allObjectivesOf(UUID teacherUserId) {
        return objectives.findByLevels(levels.findByTeacher(teacherUserId).stream().map(Level::id).toList()).stream()
                .map(ObjectiveView::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ObjectiveView> liveObjective(UUID objectiveId, UUID teacherUserId) {
        return findOwnedObjective(teacherUserId, objectiveId).map(ObjectiveView::from);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean belongsTo(UUID levelId, UUID teacherUserId) {
        return levels.findById(levelId).filter(level -> level.teacherUserId().equals(teacherUserId)).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public String nameOf(UUID levelId) {
        return levels.findById(levelId).map(Level::name).orElse(null);
    }

    @Override
    @Transactional
    public void seedDefaults(UUID teacherUserId) {
        if (!levels.findByTeacher(teacherUserId).isEmpty()) {
            return;
        }
        for (int i = 0; i < Level.DEFAULTS.size(); i++) {
            levels.save(new Level(null, teacherUserId, Level.DEFAULTS.get(i), i + 1));
        }
    }

    /** The level goes to {@code position} (clamped to the list) and the rest keep their order around it. */
    private Level moveTo(UUID teacherUserId, Level level, int position) {
        List<Level> ordered = new ArrayList<>(levels.findByTeacher(teacherUserId));
        ordered.removeIf(other -> other.id().equals(level.id()));
        int index = Math.clamp(position - 1L, 0, ordered.size());
        ordered.add(index, level);
        return renumber(ordered).get(index);
    }

    private List<Level> renumber(List<Level> ordered) {
        List<Level> saved = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Level level = ordered.get(i);
            saved.add(level.position() == i + 1 ? level : levels.save(level.at(i + 1)));
        }
        return saved;
    }

    private List<LevelObjective> liveOf(UUID levelId) {
        return objectives.findByLevels(List.of(levelId)).stream().filter(objective -> !objective.isRetired()).toList();
    }

    /** Like {@link #moveTo(UUID, Level, int)}, among the live objectives of the same level. */
    private LevelObjective moveTo(LevelObjective objective, int position) {
        List<LevelObjective> ordered = new ArrayList<>(liveOf(objective.levelId()));
        ordered.removeIf(other -> other.id().equals(objective.id()));
        int index = Math.clamp(position - 1L, 0, ordered.size());
        ordered.add(index, objective);
        return renumberObjectives(ordered).get(index);
    }

    private List<LevelObjective> renumberObjectives(List<LevelObjective> ordered) {
        List<LevelObjective> saved = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            LevelObjective objective = ordered.get(i);
            saved.add(objective.position() == i + 1 ? objective : objectives.save(objective.at(i + 1)));
        }
        return saved;
    }

    /** A retired objective, or one on another teacher's level, answers like one that does not exist. */
    private LevelObjective ownedObjective(UUID teacherUserId, UUID objectiveId) {
        return findOwnedObjective(teacherUserId, objectiveId)
                .orElseThrow(() -> new ObjectiveNotFoundException("No objective with id " + objectiveId));
    }

    private Optional<LevelObjective> findOwnedObjective(UUID teacherUserId, UUID objectiveId) {
        return objectives.findById(objectiveId)
                .filter(objective -> !objective.isRetired())
                .filter(objective -> belongsTo(objective.levelId(), teacherUserId));
    }

    /** Another teacher's level answers like one that does not exist. */
    private Level owned(UUID teacherUserId, UUID levelId) {
        return levels.findById(levelId)
                .filter(level -> level.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new LevelNotFoundException("No level with id " + levelId));
    }
}
