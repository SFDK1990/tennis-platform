package com.tennisplatform.identity.application.port.out;

/**
 * Deterministic hash for opaque tokens. Unlike passwords, these must be looked up by hash
 * through a unique index, so a randomly salted algorithm cannot be used. Safe here because
 * the tokens carry full machine-generated entropy rather than human-chosen secrets.
 */
public interface TokenHasher {

    String hash(String rawToken);
}
