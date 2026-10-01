package com.tennisplatform.identity.domain;

public enum UserStatus {

    /** Registered but the email address has not been confirmed yet. May sign in, may not book. */
    PENDING_VERIFICATION,

    /** Email confirmed. Full access according to the account's role. */
    ACTIVE,

    /** Deactivated by an administrator. May not sign in at all. */
    DISABLED,

    /**
     * The student deleted their account: the address and the password are gone, so nobody can
     * sign in, and unlike {@link #DISABLED} there is nobody to give it back to. Final.
     */
    DELETED
}
