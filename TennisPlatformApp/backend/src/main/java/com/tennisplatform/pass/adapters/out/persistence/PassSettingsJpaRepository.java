package com.tennisplatform.pass.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PassSettingsJpaRepository extends JpaRepository<PassSettingsEntity, UUID> {
}
