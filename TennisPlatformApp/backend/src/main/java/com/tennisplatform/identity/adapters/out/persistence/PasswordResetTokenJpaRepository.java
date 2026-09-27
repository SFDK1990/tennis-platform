package com.tennisplatform.identity.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.Optional;
import java.util.UUID;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetTokenEntity t set t.usedAt = :now "
            + "where t.userId = :userId and t.usedAt is null")
    int spendAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
