package com.tennisplatform.identity.application.port.out;

public interface SecureTokenGenerator {

    /** A fresh, unguessable, URL-safe token. */
    String generate();
}
