package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.identity.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.id();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash();
        entity.role = user.role();
        entity.status = user.status();
        entity.emailVerifiedAt = user.emailVerifiedAt();
        entity.createdAt = user.createdAt();
        return entity;
    }

    User toDomain() {
        return User.rehydrate(id, new EmailAddress(email), passwordHash, role, status,
                emailVerifiedAt, createdAt);
    }
}
