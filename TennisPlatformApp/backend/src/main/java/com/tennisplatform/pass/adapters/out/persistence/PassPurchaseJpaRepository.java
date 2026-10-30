package com.tennisplatform.pass.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PassPurchaseJpaRepository extends JpaRepository<PassPurchaseEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PassPurchaseEntity p where p.checkoutSessionId = :sessionId")
    Optional<PassPurchaseEntity> lockByCheckoutSessionId(@Param("sessionId") String checkoutSessionId);

    List<PassPurchaseEntity> findAllByStudentUserIdOrderByCreatedAtDesc(UUID studentUserId);
}
