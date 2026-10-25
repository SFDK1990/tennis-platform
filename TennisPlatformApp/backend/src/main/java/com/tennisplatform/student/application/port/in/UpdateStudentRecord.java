package com.tennisplatform.student.application.port.in;

import java.util.UUID;

/**
 * The teacher writes their record of a managed student (36-fase23-analisis-niveles-ficha.md):
 * the three fields are replaced together, and a null level means none.
 */
public interface UpdateStudentRecord {

    ManagedStudentDetailView update(UUID teacherUserId, UUID studentUserId, UUID levelId, String goals,
                                    String privateNotes);
}
