package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.out.CourtRepository;
import com.tennisplatform.lesson.domain.Court;
import com.tennisplatform.lesson.domain.CourtNotFoundException;

import java.util.UUID;

/** The court a lesson asks for has to exist; none means the first one (Fase 25.6). */
final class LessonCourts {

    private LessonCourts() {
    }

    static void requireExisting(CourtRepository courts, UUID courtId) {
        if (courtId != null) {
            find(courts, courtId);
        }
    }

    static Court find(CourtRepository courts, UUID courtId) {
        return courts.findById(courtId).orElseThrow(() -> new CourtNotFoundException("No court with id " + courtId));
    }
}
