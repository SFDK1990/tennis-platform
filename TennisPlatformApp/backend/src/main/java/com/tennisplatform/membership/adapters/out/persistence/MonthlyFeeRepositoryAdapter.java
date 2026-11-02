package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.application.port.out.MonthlyFeeRepository;
import com.tennisplatform.membership.domain.FeeAlreadyRecordedException;
import com.tennisplatform.membership.domain.MonthlyFee;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class MonthlyFeeRepositoryAdapter implements MonthlyFeeRepository {

    private final MonthlyFeeJpaRepository jpa;

    MonthlyFeeRepositoryAdapter(MonthlyFeeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MonthlyFee save(MonthlyFee fee) {
        try {
            return jpa.saveAndFlush(MonthlyFeeEntity.fromDomain(fee)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.is(e, ConstraintViolations.UNIQUE_VIOLATION)) {
                throw new FeeAlreadyRecordedException("This month already has a fee recorded: void it first");
            }
            throw e;
        }
    }

    @Override
    public Optional<MonthlyFee> findById(UUID id) {
        return jpa.findById(id).map(MonthlyFeeEntity::toDomain);
    }

    @Override
    public List<MonthlyFee> findLiveOf(Collection<UUID> membershipIds) {
        if (membershipIds.isEmpty()) {
            return List.of();
        }
        return jpa.findAllByMembershipIdInAndVoidedAtIsNull(membershipIds).stream()
                .map(MonthlyFeeEntity::toDomain).toList();
    }

    @Override
    public List<MonthlyFee> findPaidBetween(UUID teacherUserId, LocalDate from, LocalDate until) {
        return jpa.findPaidBetween(teacherUserId, from, until).stream().map(MonthlyFeeEntity::toDomain).toList();
    }
}
