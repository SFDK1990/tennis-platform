package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.application.port.out.PassPurchaseRepository;
import com.tennisplatform.pass.domain.PassPurchase;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PassPurchaseRepositoryAdapter implements PassPurchaseRepository {

    private final PassPurchaseJpaRepository jpa;

    PassPurchaseRepositoryAdapter(PassPurchaseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PassPurchase> findById(UUID id) {
        return jpa.findById(id).map(PassPurchaseEntity::toDomain);
    }

    @Override
    public Optional<PassPurchase> lockByCheckoutSessionId(String checkoutSessionId) {
        return jpa.lockByCheckoutSessionId(checkoutSessionId).map(PassPurchaseEntity::toDomain);
    }

    @Override
    public List<PassPurchase> findByStudent(UUID studentUserId) {
        return jpa.findAllByStudentUserIdOrderByCreatedAtDesc(studentUserId).stream()
                .map(PassPurchaseEntity::toDomain)
                .toList();
    }

    @Override
    public PassPurchase save(PassPurchase purchase) {
        return jpa.saveAndFlush(PassPurchaseEntity.fromDomain(purchase)).toDomain();
    }
}
