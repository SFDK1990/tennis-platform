package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.GetCurrentUser;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.InvalidCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class GetCurrentUserService implements GetCurrentUser {

    private final UserRepository users;

    public GetCurrentUserService(UserRepository users) {
        this.users = users;
    }

    /**
     * Reads the account rather than trusting the token's claims: the token may be up to its
     * whole lifetime out of date, and this endpoint is what a client uses to learn its current
     * state (for example, that the email has just been verified).
     */
    @Override
    @Transactional(readOnly = true)
    public UserSummary byId(UUID userId) {
        return users.findById(userId)
                .map(UserSummary::of)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
