package com.tennisplatform.identity.adapters.out.security;

import com.tennisplatform.identity.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

    /**
     * A real hash of a value nobody knows, used to spend the same CPU time when the account
     * does not exist. Computed once at startup rather than per request.
     */
    private final String decoyHash;
    private final PasswordEncoder encoder;

    BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.decoyHash = encoder.encode("not-a-real-password-" + System.nanoTime());
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    @Override
    public void burnTime(String rawPassword) {
        encoder.matches(rawPassword == null ? "" : rawPassword, decoyHash);
    }
}
