package com.tennisplatform.membership.application.port.in;

import java.util.UUID;

/** A student was signed up to a group, by themselves or by the teacher (Fase 28). Ids only. */
public record MembershipStarted(UUID membershipId, UUID groupId, UUID teacherUserId, UUID studentUserId,
                                boolean byStudent) {
}
