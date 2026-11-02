package com.tennisplatform.membership.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface FeeSettingsJpaRepository extends JpaRepository<FeeSettingsEntity, UUID> {
}
