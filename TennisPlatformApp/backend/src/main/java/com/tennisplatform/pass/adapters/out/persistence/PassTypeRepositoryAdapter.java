package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.application.port.out.PassTypeRepository;
import com.tennisplatform.pass.domain.PassType;
import com.tennisplatform.pass.domain.PassTypeAlreadyExistsException;
import com.tennisplatform.pass.domain.PassTypeInUseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PassTypeRepositoryAdapter implements PassTypeRepository {

    /** PostgreSQL SQLStates, which survive a change of wording or locale. */
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final PassTypeJpaRepository jpa;

    PassTypeRepositoryAdapter(PassTypeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<PassType> findByTeacher(UUID teacherUserId) {
        return jpa.findAllByTeacherUserId(teacherUserId).stream().map(PassTypeEntity::toDomain).toList();
    }

    @Override
    public Optional<PassType> findById(UUID id) {
        return jpa.findById(id).map(PassTypeEntity::toDomain);
    }

    @Override
    public PassType save(PassType type) {
        try {
            return jpa.saveAndFlush(PassTypeEntity.fromDomain(type)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (sqlState(e, UNIQUE_VIOLATION)) {
                throw new PassTypeAlreadyExistsException("There is already a pass type called " + type.name());
            }
            throw e;
        }
    }

    /** Flushed here, so a pass of the type is reported now and not at commit. */
    @Override
    public void delete(UUID id) {
        try {
            jpa.deleteById(id);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            if (sqlState(e, FOREIGN_KEY_VIOLATION)) {
                throw new PassTypeInUseException("A student has a pass of this type: retire it instead");
            }
            throw e;
        }
    }

    private static boolean sqlState(DataIntegrityViolationException e, String state) {
        return e.getMostSpecificCause() instanceof SQLException cause && state.equals(cause.getSQLState());
    }
}
