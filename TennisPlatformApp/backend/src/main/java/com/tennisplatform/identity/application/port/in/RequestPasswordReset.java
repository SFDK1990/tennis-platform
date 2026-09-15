package com.tennisplatform.identity.application.port.in;

public interface RequestPasswordReset {

    /** Always succeeds, whether or not the address belongs to an account. */
    void request(Command command);

    record Command(String email) {
    }
}
