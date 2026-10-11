package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.PasswordPolicy;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class ProvisionTeacherAccountService implements ProvisionTeacherAccount {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public ProvisionTeacherAccountService(UserRepository users, PasswordHasher passwordHasher,
                                          Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UUID provision(String email, String rawPassword) {
        // The partial unique index would reject a second teacher anyway; checking first turns a
        // startup crash into a no-op on every restart.
        return users.findTheTeacher()
                .map(User::id)
                .orElseGet(() -> create(email, rawPassword));
    }

    private UUID create(String email, String rawPassword) {
        EmailAddress address = new EmailAddress(email);
        // Without this the unique index answers, and the startup failure names the constraint
        // instead of the variable to fix. The address itself stays out of the message.
        if (users.findByEmail(address).isPresent()) {
            throw new IllegalStateException("TEACHER_EMAIL already belongs to another account");
        }
        PasswordPolicy.validate(rawPassword);
        User teacher = users.save(User.bootstrapTeacher(
                address,
                passwordHasher.hash(rawPassword),
                clock.instant()));
        return teacher.id();
    }
}
