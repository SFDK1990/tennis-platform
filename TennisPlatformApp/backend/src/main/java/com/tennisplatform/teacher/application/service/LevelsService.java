package com.tennisplatform.teacher.application.service;

import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ManageLevels;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import com.tennisplatform.teacher.application.port.out.LevelRepository;
import com.tennisplatform.teacher.domain.Level;
import com.tennisplatform.teacher.domain.LevelNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LevelsService implements ManageLevels, QueryLevels {

    private static final Logger log = LoggerFactory.getLogger(LevelsService.class);

    private final LevelRepository levels;
    private final GetTeacherProfile teacherProfile;

    public LevelsService(LevelRepository levels, GetTeacherProfile teacherProfile) {
        this.levels = levels;
        this.teacherProfile = teacherProfile;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LevelView> ofTheTeacher() {
        return levels.findByTeacher(teacherProfile.get().userId()).stream().map(LevelView::from).toList();
    }

    @Override
    @Transactional
    public LevelView create(UUID teacherUserId, String name) {
        List<Level> existing = levels.findByTeacher(teacherUserId);
        Level saved = levels.save(new Level(null, teacherUserId, name, existing.size() + 1));
        log.info("Level {} created", saved.id());
        return LevelView.from(saved);
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
        return LevelView.from(level);
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

    /** Another teacher's level answers like one that does not exist. */
    private Level owned(UUID teacherUserId, UUID levelId) {
        return levels.findById(levelId)
                .filter(level -> level.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new LevelNotFoundException("No level with id " + levelId));
    }
}
