package com.tennisplatform.student.application.port.in;

import java.util.UUID;

/**
 * The teacher marked an objective as achieved for a student who did not have it (Fase 29, decision
 * 5). Ids only: whoever hears it reads the rest after the commit.
 */
public record ObjectiveAchieved(UUID teacherUserId, UUID studentUserId, UUID objectiveId) {
}
