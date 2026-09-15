package com.tennisplatform.identity.application.port.in;

public interface RefreshSession {

    AuthenticationResult refresh(Command command);

    record Command(String refreshToken) {
    }
}
