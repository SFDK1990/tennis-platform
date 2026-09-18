package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(EmailAddress email);

    Optional<User> findById(UUID id);

    boolean teacherExists();

    /**
     * The single teacher account. Meaningful because a partial unique index on {@code users}
     * guarantees there is at most one; the day there are several, this is one of the places
     * that has to change rather than silently return an arbitrary row.
     */
    Optional<User> findTheTeacher();

    User save(User user);
}
