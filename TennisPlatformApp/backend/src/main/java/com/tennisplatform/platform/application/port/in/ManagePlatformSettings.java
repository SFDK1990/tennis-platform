package com.tennisplatform.platform.application.port.in;

import java.util.UUID;

/**
 * Reading and changing the configuration, for the administration console. Who may call it is
 * decided by {@code administration}; this module only keeps the values valid.
 */
public interface ManagePlatformSettings {

    PlatformSettingsView current();

    /** A null value keeps the current one. Both, when sent, must be positive. */
    PlatformSettingsView change(UUID administrator, Integer studentLimit, Integer maxGroupCapacity);
}
