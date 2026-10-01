package com.tennisplatform.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An account. Transitions are methods rather than setters so the valid state machine
 * lives here and cannot be bypassed by an adapter.
 */
public class User {

    private final UUID id;
    private EmailAddress email;
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
        if (!canAuthenticate()) {
            throw new AccountNotActiveException("A disabled account cannot be verified");
        }
        if (emailVerifiedAt == null) {
            this.emailVerifiedAt = now;
        }
        this.status = UserStatus.ACTIVE;
    }

    public void changePassword(String newPasswordHash) {
        if (!canAuthenticate()) {
            throw new AccountNotActiveException("A disabled account cannot change its password");
        }
        this.passwordHash = newPasswordHash;
    }

    public void disable() {
        refuseIfDeleted();
        this.status = UserStatus.DISABLED;
    }

    /**
     * The student deleted their account (30-fase19-analisis-cierre-mvp.md). The row survives,
     * because past bookings point at it, but nothing in it identifies anybody any more: the
     * address becomes one that cannot exist and frees the real one for a new registration, and
     * the password hash becomes one no password matches.
     */
    public void close(String unusablePasswordHash) {
        refuseIfDeleted();
        this.email = new EmailAddress("deleted-" + id + "@account.invalid");
        this.passwordHash = unusablePasswordHash;
        this.status = UserStatus.DELETED;
    }

    /**
     * Back to what the account was before it was disabled: active if the address had been
     * verified, pending otherwise. Reactivating must not verify an address nobody confirmed.
     */
    public void reactivate() {
        refuseIfDeleted();
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
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING_VERIFICATION;
    }

    public boolean isDeleted() {
        return status == UserStatus.DELETED;
    }

    private void refuseIfDeleted() {
        if (isDeleted()) {
            throw new AccountDeletedException();
        }
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
