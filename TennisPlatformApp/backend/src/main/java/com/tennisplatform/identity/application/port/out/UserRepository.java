package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(EmailAddress email);

    Optional<User> findById(UUID id);

    /**
     * The accounts with these ids, skipping the ones that do not exist. In bulk because the
     * alternative is one query per row when another module resolves the addresses of a listing.
     */
    List<User> findAllById(Collection<UUID> ids);

    boolean teacherExists();

    /**
     * The single teacher account. Meaningful because a partial unique index on {@code users}
     * guarantees there is at most one; the day there are several, this is one of the places
     * that has to change rather than silently return an arbitrary row.
     */
    Optional<User> findTheTeacher();

    User save(User user);
}
