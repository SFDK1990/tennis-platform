package com.tennisplatform.lesson.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The dates of a lesson repeated every week (35-fase22-analisis-clases-que-se-repiten.md).
 *
 * <p>Each week keeps the first lesson's wall-clock time in the teacher's zone, not its instant:
 * a lesson on Tuesdays at 18:00 is still at 18:00 after the clocks change, like the teacher's
 * availability. Seven days of instants would move it an hour. The length is kept in instants,
 * as for any lesson.
 */
public final class WeeklySeries {

    /** A four-month term: the whole course at once, and not a year of lessons by mistake. */
    public static final int MAX_WEEKS = 16;

    private WeeklySeries() {
    }

    public static List<LessonPeriod> occurrences(Instant startsAt, Instant endsAt, ZoneId zone, int weeks) {
        if (weeks < 2 || weeks > MAX_WEEKS) {
            throw new InvalidLessonException("A series runs from 2 to " + MAX_WEEKS + " weeks, not " + weeks);
        }
        LessonPeriod first = new LessonPeriod(startsAt, endsAt);
        Duration length = Duration.between(first.startsAt(), first.endsAt());
        ZonedDateTime start = first.startsAt().atZone(zone);
        return IntStream.range(0, weeks)
                .mapToObj(week -> {
                    Instant weekStart = start.plusWeeks(week).toInstant();
                    return new LessonPeriod(weekStart, weekStart.plus(length));
                })
                .toList();
    }
}
