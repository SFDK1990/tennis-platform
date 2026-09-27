package com.tennisplatform.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An account. Transitions are methods rather than setters so the valid state machine
 * lives here and cannot be bypassed by an adapter.
 */
public class User {

    private final UUID id;
    private final EmailAddress email;
    private String passwordHash;
    private final Role role;
    private UserStatus status;
    private Instant emailVerifiedAt;
    private final Instant createdAt;

    private User(UUID id, EmailAddress email, String passwordHash, Role role,
                 UserStatus status, Instant emailVerifiedAt, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
        this.createdAt = createdAt;
    }

    /** Public registration. Always produces a STUDENT: the client never chooses a role. */
    public static User register(EmailAddress email, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, Role.STUDENT,
                UserStatus.PENDING_VERIFICATION, null, now);
    }

    /** The bootstrapped teacher account, created already verified. */
    public static User bootstrapTeacher(EmailAddress email, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, Role.TEACHER,
                UserStatus.ACTIVE, now, now);
    }

    /** The bootstrapped administrator, created already verified, like the teacher. */
    public static User bootstrapAdmin(EmailAddress email, String passwordHash, Instant now) {
        return new User(UUID.randomUUID(), email, passwordHash, Role.ADMIN,
                UserStatus.ACTIVE, now, now);
    }

    /** Rehydration from persistence. */
    public static User rehydrate(UUID id, EmailAddress email, String passwordHash, Role role,
                                 UserStatus status, Instant emailVerifiedAt, Instant createdAt) {
        return new User(id, email, passwordHash, role, status, emailVerifiedAt, createdAt);
    }

    public void verifyEmail(Instant now) {
        if (status == UserStatus.DISABLED) {
            throw new AccountNotActiveException("A disabled account cannot be verified");
        }
        if (emailVerifiedAt == null) {
            this.emailVerifiedAt = now;
        }
        this.status = UserStatus.ACTIVE;
    }

    public void changePassword(String newPasswordHash) {
        if (status == UserStatus.DISABLED) {
            throw new AccountNotActiveException("A disabled account cannot change its password");
        }
        this.passwordHash = newPasswordHash;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    /**
     * Back to what the account was before it was disabled: active if the address had been
     * verified, pending otherwise. Reactivating must not verify an address nobody confirmed.
     */
    public void reactivate() {
        if (status == UserStatus.DISABLED) {
            this.status = emailVerifiedAt != null ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION;
        }
    }

    /**
     * A pending-verification account is deliberately allowed to sign in: verification gates
     * booking, not access (see 13-fase5-analisis-identity.md). Otherwise a lost email would
     * lock the user out entirely.
     */
    public boolean canAuthenticate() {
        return status != UserStatus.DISABLED;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public UUID id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    public Instant emailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
