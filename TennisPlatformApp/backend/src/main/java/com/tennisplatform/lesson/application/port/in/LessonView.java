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
                         boolean cancelledAtShortNotice) {

    public static LessonView from(Lesson lesson, Instant now, int bookedCount) {
        return new LessonView(lesson.id(), lesson.teacherUserId(), lesson.type().name(),
                lesson.startsAt(), lesson.endsAt(), lesson.capacity(), bookedCount,
                lesson.statusAt(now, bookedCount).name(), lesson.notes(),
                lesson.createdOutsideAvailability(), lesson.cancelledAt(),
                lesson.cancelledAtShortNotice());
    }
}
