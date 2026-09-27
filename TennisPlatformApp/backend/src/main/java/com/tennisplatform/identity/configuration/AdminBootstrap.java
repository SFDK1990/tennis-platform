package com.tennisplatform.identity.configuration;

import com.tennisplatform.identity.application.service.ProvisionAdminAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Creates the first administrator from ADMIN_EMAIL and ADMIN_PASSWORD, the way
 * {@code TeacherBootstrap} creates the teacher. Unlike the teacher it lives in identity: an
 * administrator is only an account, with no profile in another module.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final ProvisionAdminAccountService admins;
    private final IdentityProperties properties;

    AdminBootstrap(ProvisionAdminAccountService admins, IdentityProperties properties) {
        this.admins = admins;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<String> email = trimmed(properties.getAdminBootstrapEmail());
        Optional<String> password = trimmed(properties.getAdminBootstrapPassword());
        if (email.isEmpty() || password.isEmpty()) {
            log.info("Admin bootstrap skipped: no bootstrap credentials configured");
            return;
        }
        // The address is not logged: it is personal data (02-arquitectura.md).
        log.info(admins.provision(email.get(), password.get())
                ? "Admin account created by bootstrap"
                : "Admin bootstrap: the account already exists, nothing to do");
    }

    private static Optional<String> trimmed(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(s -> !s.isEmpty());
    }
}
