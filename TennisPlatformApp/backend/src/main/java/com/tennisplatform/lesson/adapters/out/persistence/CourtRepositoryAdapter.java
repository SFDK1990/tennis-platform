package com.tennisplatform.lesson.adapters.out.persistence;

import com.tennisplatform.lesson.application.port.out.CourtRepository;
import com.tennisplatform.lesson.domain.Court;
import com.tennisplatform.lesson.domain.CourtAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CourtRepositoryAdapter implements CourtRepository {

    /** PostgreSQL's SQLState for a unique violation: the name is unique. */
    private static final String UNIQUE_VIOLATION = "23505";

    private final CourtJpaRepository jpa;

    CourtRepositoryAdapter(CourtJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Court> findAll() {
        return jpa.findAllByOrderByPositionAsc().stream().map(CourtEntity::toDomain).toList();
    }

    @Override
    public Optional<Court> findById(UUID id) {
        return jpa.findById(id).map(CourtEntity::toDomain);
    }

    @Override
    public Court save(Court court) {
        try {
            return jpa.saveAndFlush(CourtEntity.fromDomain(court)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() instanceof SQLException cause && UNIQUE_VIOLATION.equals(cause.getSQLState())) {
                throw new CourtAlreadyExistsException("There is already a court called " + court.name());
            }
            throw e;
        }
    }
}
