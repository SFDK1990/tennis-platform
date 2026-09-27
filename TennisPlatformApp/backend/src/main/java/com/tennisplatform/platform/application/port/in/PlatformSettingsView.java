package com.tennisplatform.platform.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** The configuration as the console shows it. {@code updatedBy} is null until somebody changes it. */
public record PlatformSettingsView(int studentLimit, int maxGroupCapacity, Instant updatedAt, UUID updatedBy) {
}
