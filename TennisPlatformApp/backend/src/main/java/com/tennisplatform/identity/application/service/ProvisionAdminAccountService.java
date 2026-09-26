package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.PasswordPolicy;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Creates the first administrator at startup. The public registration never can
 * (01-analisis-funcional.md §3), and one created by hand in the database is exactly what a
 * deployment cannot repeat.
 */
public class ProvisionAdminAccountService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public ProvisionAdminAccountService(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    /**
     * Returns whether an account was created. Idempotent: it runs on every startup. An account
     * that already has the address is left untouched - its role and its password included - so
     * changing the variables later can never take over an account in use.
     */
    @Transactional
    public boolean provision(String email, String rawPassword) {
        EmailAddress address = new EmailAddress(email);
        if (users.findByEmail(address).isPresent()) {
            return false;
        }
        PasswordPolicy.validate(rawPassword);
        users.save(User.bootstrapAdmin(address, passwordHasher.hash(rawPassword), clock.instant()));
        return true;
    }
}
