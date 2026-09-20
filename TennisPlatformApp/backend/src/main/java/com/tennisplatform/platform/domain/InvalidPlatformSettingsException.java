package com.tennisplatform.platform.domain;

/** The submitted configuration breaks a rule of the domain. Maps to 400. */
public class InvalidPlatformSettingsException extends RuntimeException {

    public InvalidPlatformSettingsException(String message) {
        super(message);
    }
}
