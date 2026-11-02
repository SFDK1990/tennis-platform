package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.application.port.out.FixedGroupRepository;
import com.tennisplatform.membership.domain.FixedGroup;
import com.tennisplatform.membership.domain.GroupAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class FixedGroupRepositoryAdapter implements FixedGroupRepository {

    private final FixedGroupJpaRepository jpa;

    FixedGroupRepositoryAdapter(FixedGroupJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FixedGroup save(FixedGroup group) {
        try {
            return jpa.saveAndFlush(FixedGroupEntity.fromDomain(group)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.is(e, ConstraintViolations.UNIQUE_VIOLATION)) {
                throw new GroupAlreadyExistsException("There is already a group called " + group.name());
            }
            throw e;
        }
    }

    @Override
    public Optional<FixedGroup> findById(UUID id) {
        return jpa.findById(id).map(FixedGroupEntity::toDomain);
    }

    @Override
    public List<FixedGroup> findByTeacher(UUID teacherUserId) {
        return jpa.findAllByTeacherUserIdOrderByNameAsc(teacherUserId).stream().map(FixedGroupEntity::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
        jpa.flush();
    }
}
