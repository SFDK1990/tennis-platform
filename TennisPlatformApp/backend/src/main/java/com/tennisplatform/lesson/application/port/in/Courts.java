package com.tennisplatform.lesson.application.port.in;

import java.util.List;
import java.util.UUID;

/** The school's courts (Fase 25.6): anybody signed in reads them, only the teacher renames them. */
public interface Courts {

    /** In their order: the first one is where a lesson goes by default. */
    List<CourtView> all();

    CourtView rename(UUID teacherUserId, UUID courtId, String name);

    record CourtView(UUID id, String name, int position) {
    }
}
