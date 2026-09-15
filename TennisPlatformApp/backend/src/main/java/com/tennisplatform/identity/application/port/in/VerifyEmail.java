package com.tennisplatform.identity.application.port.in;

public interface VerifyEmail {

    void verify(Command command);

    record Command(String token) {
    }
}
