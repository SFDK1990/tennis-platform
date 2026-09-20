package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return jpa.findByEmail(email.value()).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(UserEntity::toDomain).toList();
    }

    @Override
    public boolean teacherExists() {
        return jpa.existsByRole(Role.TEACHER);
    }

    @Override
    public Optional<User> findTheTeacher() {
        return jpa.findFirstByRole(Role.TEACHER).map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        return jpa.save(UserEntity.fromDomain(user)).toDomain();
    }
}
