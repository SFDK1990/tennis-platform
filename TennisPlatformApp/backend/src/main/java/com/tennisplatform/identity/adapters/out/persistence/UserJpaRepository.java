package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByRole(Role role);

    Optional<UserEntity> findFirstByRole(Role role);
}
