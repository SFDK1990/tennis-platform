package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.lesson.application.port.in.LessonView;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
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

    /** The reason goes to the students' email and is not stored (Fase 20). */
    record CancelLessonRequest(@Size(max = 200) String reason) {
    }

    /** A day in the teacher's zone, and the reason for the students. */
    record CancelDayRequest(@NotNull LocalDate date, @Size(max = 200) String reason) {
    }

    record CancelledDayResponse(List<LessonResponse> lessons) {
    }

    /** Every field optional: this is a PATCH, and null means "leave as it is". */
    record EditLessonRequest(@Size(max = 2000) String notes, @Positive Integer capacity) {
    }

    record CreateLessonRequest(@NotNull String type, @NotNull Instant startsAt, @NotNull Instant endsAt,
                               int capacity, String notes, boolean overrideAvailability, UUID levelId,
                               UUID courtId) {
    }

    /** The level the lesson is for, or null for everybody (Fase 23). */
    record ChangeLevelRequest(UUID levelId) {
    }

    /** The court a lesson moves to (Fase 25.6). */
    record ChangeCourtRequest(@NotNull UUID courtId) {
    }

    /** A court's new name (Fase 25.6). */
    record RenameCourtRequest(@NotNull String name) {
    }

    /** The first lesson of a weekly series and how many weeks it runs (Fase 22). */
    record SeriesPreviewRequest(@NotNull String type, @NotNull Instant startsAt, @NotNull Instant endsAt,
                                int capacity, String notes, int weeks, UUID levelId, UUID courtId) {
    }

    record SeriesPreviewResponse(List<OccurrenceResponse> occurrences) {
    }

    record OccurrenceResponse(Instant startsAt, Instant endsAt, String status) {
    }

    /** The same series, and the dates the teacher kept. */
    record CreateSeriesRequest(@NotNull String type, @NotNull Instant startsAt, @NotNull Instant endsAt,
                               int capacity, String notes, int weeks, @NotNull List<@NotNull Instant> dates,
                               boolean overrideAvailability, UUID levelId, UUID courtId) {
    }

    record LessonsResponse(List<LessonResponse> lessons) {
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
                          boolean cancelledAtShortNotice, UUID seriesId, Integer seriesPosition,
                          Integer seriesSize, UUID levelId, UUID courtId) {

        static LessonResponse from(LessonView lesson, boolean forTheTeacher) {
            return new LessonResponse(lesson.id(), lesson.teacherUserId(), lesson.type(),
                    lesson.startsAt(), lesson.endsAt(), lesson.capacity(), lesson.bookedCount(),
                    lesson.status(),
                    forTheTeacher ? lesson.notes() : null, lesson.createdOutsideAvailability(),
                    lesson.cancelledAt(), lesson.cancelledAtShortNotice(), lesson.seriesId(),
                    lesson.seriesPosition(), lesson.seriesSize(), lesson.levelId(), lesson.courtId());
        }
    }
}
