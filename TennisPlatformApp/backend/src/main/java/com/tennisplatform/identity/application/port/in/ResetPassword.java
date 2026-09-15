package com.tennisplatform.identity.application.port.in;

public interface ResetPassword {

    void reset(Command command);

    record Command(String token, String newPassword) {
    }
}
