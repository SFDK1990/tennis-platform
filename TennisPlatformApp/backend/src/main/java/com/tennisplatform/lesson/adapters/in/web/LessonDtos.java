package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.lesson.application.port.in.LessonView;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The wire shapes of this module, grouped like {@code TeacherDtos} and {@code AvailabilityDtos}.
 *
 * <p>Validation of the values themselves lives in the domain, which is where the rules are; the
 * annotations here only guard the fields the service would otherwise dereference before the
 * domain ever sees them.
 */
final class LessonDtos {

    private LessonDtos() {
    }

    record CreateLessonRequest(@NotNull String type, @NotNull Instant startsAt, @NotNull Instant endsAt,
                               int capacity, String notes, boolean overrideAvailability) {
    }

    /**
     * A lesson on the wire.
     *
     * <p>{@code bookedCount} is the number of confirmed bookings. It was left out of Fase 8 on
     * purpose - only {@code booking} can count seats, and a constant zero would have looked like a
     * number - and came back with Fase 9.
     *
     * <p>{@code notes} is null for everyone but the teacher: it is a free-text field where "work
     * on the backhand" and anything else the teacher writes to themselves ends up, and it was
     * not written for the student to read.
     */
    record LessonResponse(UUID id, UUID teacherUserId, String type, Instant startsAt, Instant endsAt,
                          int capacity, int bookedCount, String status, String notes,
                          boolean createdOutsideAvailability, Instant cancelledAt,
                          boolean cancelledAtShortNotice) {

        static LessonResponse from(LessonView lesson, boolean forTheTeacher) {
            return new LessonResponse(lesson.id(), lesson.teacherUserId(), lesson.type(),
                    lesson.startsAt(), lesson.endsAt(), lesson.capacity(), lesson.bookedCount(),
                    lesson.status(),
                    forTheTeacher ? lesson.notes() : null, lesson.createdOutsideAvailability(),
                    lesson.cancelledAt(), lesson.cancelledAtShortNotice());
        }
    }

    record LessonsResponse(List<LessonResponse> items) {

        static LessonsResponse of(List<LessonView> lessons) {
            return new LessonsResponse(lessons.stream()
                    .map(lesson -> LessonResponse.from(lesson, true))
                    .toList());
        }
    }
}
