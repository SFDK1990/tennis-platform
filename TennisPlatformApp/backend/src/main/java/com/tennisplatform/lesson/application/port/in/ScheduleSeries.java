package com.tennisplatform.lesson.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The same lesson every week for several weeks (35-fase22-analisis-clases-que-se-repiten.md).
 * First the dates are shown with what is wrong with each, then the teacher creates the ones they
 * chose.
 */
public interface ScheduleSeries {

    /** Nothing is saved: every date of the series and whether it can be created. */
    List<Occurrence> preview(UUID teacherUserId, NewSeries series);

    /**
     * Creates the chosen dates, all of them or none. Each has to be one of the series' dates; one
     * that now clashes with another lesson fails the whole series.
     */
    List<LessonView> schedule(UUID teacherUserId, NewSeries series, List<Instant> chosen, boolean overrideAvailability);

    /** The first lesson of the series, and how many weeks it runs counting that one. */
    record NewSeries(String type, Instant startsAt, Instant endsAt, int capacity, String notes, int weeks,
                     UUID levelId) {
    }

    /** {@code status} is OK, OUTSIDE_AVAILABILITY, OVERLAP or IN_THE_PAST. */
    record Occurrence(Instant startsAt, Instant endsAt, String status) {
    }
}
