package com.tennisplatform.identity.configuration;

import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.PasswordPolicy;
import com.tennisplatform.identity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Creates the single teacher account at startup from environment variables.
 *
 * <p>This replaces the "seed from a migration" approach the architecture originally proposed.
 * A Liquibase changeset is a file committed to a repository that is already published on
 * GitHub: putting the teacher's initial password there - in clear text or hashed - would
 * leave it in the history permanently, and a BCrypt hash in a repository is material an
 * attacker can work on offline at leisure.
 *
 * <p>Idempotent by design: running it twice never creates a second teacher, which matters
 * because it executes on every single startup.
 */
@Component
class TeacherBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TeacherBootstrap.class);

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final IdentityProperties properties;
    private final Clock clock;

    TeacherBootstrap(UserRepository users, PasswordHasher passwordHasher,
                     IdentityProperties properties, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<String> email = trimmed(properties.getBootstrapTeacherEmail());
        Optional<String> password = trimmed(properties.getBootstrapTeacherPassword());

        if (email.isEmpty() || password.isEmpty()) {
            log.info("Teacher bootstrap skipped: no bootstrap credentials configured");
            return;
        }

        // The partial unique index on users would reject a second teacher anyway; checking
        // first turns a startup crash into a no-op on every restart.
        if (users.teacherExists()) {
            log.info("Teacher bootstrap skipped: a teacher account already exists");
            return;
        }

        PasswordPolicy.validate(password.get());
        users.save(User.bootstrapTeacher(
                new EmailAddress(email.get()),
                passwordHasher.hash(password.get()),
                clock.instant()));

        // The address is not logged: it is personal data (08-security-engineer.md).
        log.info("Teacher account created by bootstrap");
    }

    private Optional<String> trimmed(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(s -> !s.isEmpty());
    }
}
