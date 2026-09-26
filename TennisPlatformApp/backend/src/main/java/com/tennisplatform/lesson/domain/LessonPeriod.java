package com.tennisplatform.lesson.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * When a lesson happens, as two instants.
 *
 * <p>Instants, not wall clock - and that is the difference with an availability rule. "Mondays
 * 09:00 to 13:00" only becomes real time once a date is known; a lesson already is real time.
 *
 * <p>The consequence has to be taken with open eyes: on the two days a year the clocks change,
 * the real duration and the one the teacher sees on their own clock do not match. A lesson that
 * reads 01:30 to 03:30 on the day the clocks go forward lasts one hour, not two, and the local
 * time 02:30 that day does not exist at all. The instant is what counts, because it is what
 * actually happens; showing the resulting duration before confirming is the frontend's job.
 */
public record LessonPeriod(Instant startsAt, Instant endsAt) {

    /** Lessons are booked in half hours: the minimum is one slot and every duration is a whole number of them. */
    public static final Duration SLOT = Duration.ofMinutes(30);

    public LessonPeriod {
        if (startsAt == null || endsAt == null) {
            throw new InvalidLessonException("A lesson needs a start and an end");
        }
        if (!startsAt.isBefore(endsAt)) {
            throw new InvalidLessonException("A lesson cannot end before it starts, or last nothing");
        }
        Duration duration = Duration.between(startsAt, endsAt);
        if (duration.toMillis() % SLOT.toMillis() != 0) {
            throw new InvalidLessonException(
                    "A lesson lasts a whole number of " + SLOT.toMinutes() + "-minute slots, and this one is "
                            + duration.toMinutes() + " minutes");
        }
    }

    public Duration duration() {
        return Duration.between(startsAt, endsAt);
    }

    /**
     * Whether the lesson runs past midnight in the teacher's own zone.
     *
     * <p>Midnight is a local idea: a lesson from 23:00 to 00:30 crosses it although there is
     * nothing remarkable about those two instants in UTC. A lesson that ends exactly at midnight
     * does not cross it - it stops at the boundary, which is why the last day is measured from
     * the instant before the end rather than from the end itself.
     */
    public boolean crossesMidnightIn(ZoneId zone) {
        ZonedDateTime start = startsAt.atZone(zone);
        ZonedDateTime end = endsAt.atZone(zone);
        LocalDate lastDay = end.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? end.toLocalDate().minusDays(1)
                : end.toLocalDate();
        return !lastDay.equals(start.toLocalDate());
    }
}
