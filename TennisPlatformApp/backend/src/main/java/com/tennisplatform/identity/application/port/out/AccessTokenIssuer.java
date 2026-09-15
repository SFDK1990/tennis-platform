package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.User;

import java.time.Duration;

public interface AccessTokenIssuer {

    String issue(User user);

    /** How long the issued token stays valid; returned to the client so it can pre-empt expiry. */
    Duration lifetime();
}
