package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(EmailAddress email);

    Optional<User> findById(UUID id);

    boolean teacherExists();

    User save(User user);
}
