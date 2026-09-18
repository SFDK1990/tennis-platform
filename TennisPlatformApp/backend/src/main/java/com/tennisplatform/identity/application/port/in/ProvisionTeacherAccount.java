package com.tennisplatform.identity.application.port.in;

import java.util.UUID;

/**
 * Creates the single teacher account, for the module that owns the teacher.
 *
 * <p>It exists because the account lives in {@code identity} while the profile lives in
 * {@code teacher}, and the dependency may only run one way: {@code teacher} knows
 * {@code identity}, never the reverse. Without this port the bootstrap would have to sit in
 * {@code identity} and write into {@code teacher}'s table.
 */
public interface ProvisionTeacherAccount {

    /**
     * Returns the id of the teacher account, creating it first if there is none.
     *
     * <p>Idempotent on purpose: it runs on every single startup, and a container that restarts
     * is the normal case. An existing teacher is returned untouched - in particular the password
     * is never reset, so changing the environment variable later cannot silently take over an
     * account that is already in use.
     */
    UUID provision(String email, String rawPassword);
}
