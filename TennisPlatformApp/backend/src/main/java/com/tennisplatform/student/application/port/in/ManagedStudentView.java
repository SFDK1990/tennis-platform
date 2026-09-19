package com.tennisplatform.student.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the teacher's list of students: enough to recognise somebody and no more.
 *
 * <p>{@code nationalId} and {@code address} are absent by construction, not by filtering.
 * 08-security-engineer.md forbids restricted personal data in listings, and a view that simply
 * has no field for them cannot leak them through a controller that forgets to strip them.
 *
 * <p>The status travels as a string for the same reason the identity views do it: a caller
 * outside this module would otherwise depend on its domain enum.
 */
public record ManagedStudentView(UUID userId, String email, String fullName, String managedStatus,
                                 Instant managedAt) {
}
