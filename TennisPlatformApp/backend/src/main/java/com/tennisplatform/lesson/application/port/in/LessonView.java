package com.tennisplatform.lesson.application.port.in;

import com.tennisplatform.lesson.domain.Lesson;

import java.time.Instant;
import java.util.UUID;

/**
 * A lesson as anybody outside this module sees it.
 *
 * <p>{@code type} and {@code status} travel as strings rather than as this module's enums,
 * which is the same rule the other modules' views follow: a caller that received the enum would
 * depend on this module's domain, and the boundary rules reject that - rightly, because then
 * the domain could not be reshaped without breaking them.
 *
 * <p>{@code status} is worked out at the moment of reading, so a lesson that has just finished
 * reports {@code COMPLETED} without anything having run. {@code bookedCount} counts confirmed
 * bookings only, and is what {@code FULL} is derived from. {@code notes} is included here and
 * filtered at the web edge, which is where it is known whether the caller is the teacher.
 */
public record LessonView(UUID id, UUID teacherUserId, String type, Instant startsAt, Instant endsAt,
                         int capacity, int bookedCount, String status, String notes,
                         boolean createdOutsideAvailability, Instant cancelledAt,
                         boolean cancelledAtShortNotice, UUID seriesId, Integer seriesPosition,
                         Integer seriesSize, UUID levelId) {

    public static LessonView from(Lesson lesson, Instant now, int bookedCount) {
        return new LessonView(lesson.id(), lesson.teacherUserId(), lesson.type().name(),
                lesson.startsAt(), lesson.endsAt(), lesson.capacity(), bookedCount,
                lesson.statusAt(now, bookedCount).name(), lesson.notes(),
                lesson.createdOutsideAvailability(), lesson.cancelledAt(),
                lesson.cancelledAtShortNotice(), lesson.seriesId(), null, null, lesson.levelId());
    }

    /**
     * Where the lesson stands in its weekly series, "3 of 12" (Fase 22). Only reading a single
     * lesson works it out: a list of lessons does not pay a query per series it touches.
     */
    public LessonView inSeries(int position, int size) {
        return new LessonView(id, teacherUserId, type, startsAt, endsAt, capacity, bookedCount, status, notes,
                createdOutsideAvailability, cancelledAt, cancelledAtShortNotice, seriesId, position, size, levelId);
    }
}
