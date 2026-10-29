package com.tennisplatform.lesson.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * A lesson that had not started moved to another court (Fase 25.6). Published inside the
 * transaction; whoever tells the students waits for the commit.
 */
public record LessonCourtChanged(UUID lessonId, UUID teacherUserId, String type, Instant startsAt, Instant endsAt,
                                 String courtName) {
}
