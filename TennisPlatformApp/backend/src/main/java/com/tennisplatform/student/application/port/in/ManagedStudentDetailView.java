package com.tennisplatform.student.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * The full record of one managed student, restricted personal data included.
 *
 * <p>Separate from {@link ManagedStudentView} on purpose. The listing and the detail differ in
 * exactly the fields that 02-arquitectura.md restricts, and two types make that difference
 * visible at compile time instead of depending on every call site remembering it.
 *
 * <p>Only ever built for the teacher who actually manages this student.
 */
public record ManagedStudentDetailView(UUID userId, String email, String fullName, String phone,
                                       String nationalId, String address, String managedStatus,
                                       Instant managedAt, Instant deactivatedAt) {
}
