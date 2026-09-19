package com.tennisplatform.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlatformConfigurationJpaRepository extends JpaRepository<PlatformConfigurationEntity, Short> {
}
