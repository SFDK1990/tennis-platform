package com.tennisplatform.identity.application.port.in;

import java.util.UUID;

public interface GetCurrentUser {

    UserSummary byId(UUID userId);
}
