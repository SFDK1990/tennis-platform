package com.tennisplatform.student.application.port.in;

import java.util.UUID;

/** A teacher's record of a student: level, goals and private notes (Fase 23). */
public record StudentRecordView(UUID teacherUserId, UUID levelId, String goals, String privateNotes) {
}
