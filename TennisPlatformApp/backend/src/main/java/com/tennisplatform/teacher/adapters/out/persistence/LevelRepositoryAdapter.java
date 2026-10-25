package com.tennisplatform.teacher.adapters.out.persistence;

import com.tennisplatform.teacher.application.port.out.LevelRepository;
import com.tennisplatform.teacher.domain.Level;
import com.tennisplatform.teacher.domain.LevelAlreadyExistsException;
import com.tennisplatform.teacher.domain.LevelInUseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class LevelRepositoryAdapter implements LevelRepository {

    /** PostgreSQL SQLStates, which survive a change of wording or locale. */
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final LevelJpaRepository jpa;

    LevelRepositoryAdapter(LevelJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Level> findByTeacher(UUID teacherUserId) {
        return jpa.findAllByTeacherUserIdOrderByPositionAsc(teacherUserId).stream().map(LevelEntity::toDomain).toList();
    }

    @Override
    public Optional<Level> findById(UUID id) {
        return jpa.findById(id).map(LevelEntity::toDomain);
    }

    @Override
    public Level save(Level level) {
        try {
            return jpa.saveAndFlush(LevelEntity.fromDomain(level)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (sqlState(e, UNIQUE_VIOLATION)) {
                throw new LevelAlreadyExistsException("There is already a level called " + level.name());
            }
            throw e;
        }
    }

    /** Flushed here, so a student or a lesson still on the level is reported now and not at commit. */
    @Override
    public void delete(UUID id) {
        try {
            jpa.deleteById(id);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            if (sqlState(e, FOREIGN_KEY_VIOLATION)) {
                throw new LevelInUseException("A student or a lesson still has this level");
            }
            throw e;
        }
    }

    private static boolean sqlState(DataIntegrityViolationException e, String state) {
        return e.getMostSpecificCause() instanceof SQLException cause && state.equals(cause.getSQLState());
    }
}
