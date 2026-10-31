package com.tennisplatform.membership.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * A student left a group, or the teacher took them out: their seats from {@code leftAt} on were
 * given up. Not announced when the account itself went, which already says so. Ids only.
 */
public record MembershipEnding(UUID membershipId, UUID groupId, UUID teacherUserId, UUID studentUserId,
                               Instant leftAt, boolean byStudent) {
}
