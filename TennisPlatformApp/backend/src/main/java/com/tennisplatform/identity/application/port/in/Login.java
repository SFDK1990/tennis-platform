package com.tennisplatform.identity.application.port.in;

public interface Login {

    AuthenticationResult login(Command command);

    record Command(String email, String password) {
    }
}
