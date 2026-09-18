package com.tennisplatform.teacher.configuration;

import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates the single teacher - account and profile - at startup from environment variables.
 *
 * <p>It lives in this module, not in {@code identity}, because it needs both halves and only
 * this side of the dependency graph is allowed to know both: the account is requested through
 * identity's {@link ProvisionTeacherAccount} port, and the profile is written here.
 *
 * <p>Fase 5 created only the account, leaving the teacher without the profile that
 * 10-diagrama-er.md says the bootstrap must create - and with {@code display_name} and
 * {@code timezone} being NOT NULL, no endpoint could have created it later either.
 *
 * <p>Idempotent by design, in both halves: it runs on every single startup.
 */
@Component
class TeacherBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TeacherBootstrap.class);

    private final ProvisionTeacherAccount accounts;
    private final TeacherProfileRepository profiles;
    private final TeacherProperties properties;
    private final Clock clock;

    TeacherBootstrap(ProvisionTeacherAccount accounts, TeacherProfileRepository profiles,
                     TeacherProperties properties, Clock clock) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<String> email = trimmed(properties.getBootstrapEmail());
        Optional<String> password = trimmed(properties.getBootstrapPassword());

        if (email.isEmpty() || password.isEmpty()) {
            log.info("Teacher bootstrap skipped: no bootstrap credentials configured");
            return;
        }

        UUID userId = accounts.provision(email.get(), password.get());

        if (profiles.findByUserId(userId).isPresent()) {
            log.info("Teacher bootstrap: the teacher already exists, nothing to do");
            return;
        }

        profiles.save(TeacherProfile.create(
                userId,
                trimmed(properties.getBootstrapDisplayName()).orElse("Teacher"),
                null,
                properties.getBootstrapTimezone(),
                clock.instant()));

        // Neither the address nor the name is logged: they are personal data
        // (08-security-engineer.md).
        log.info("Teacher account and profile created by bootstrap");
    }

    private Optional<String> trimmed(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(s -> !s.isEmpty());
    }
}
