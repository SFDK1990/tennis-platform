package com.tennisplatform.student.application.port.in;

import java.util.UUID;

/**
 * The answer to "who is behind this email address", which is the step before managing someone.
 *
 * <p>It is the narrowest thing that still lets a teacher confirm they are about to manage the
 * right person: no phone, no national id, no address. {@code managedStatus} is null when there
 * is no relationship yet, which is what lets the client tell "can be managed" from "already
 * managed" without a second call.
 */
public record StudentLookupView(UUID userId, String email, String fullName,
                                String managedStatus) {
}
