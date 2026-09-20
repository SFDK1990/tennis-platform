package com.tennisplatform.lesson.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final UUID TEACHER = UUID.randomUUID();
    private static final int MAX_GROUP = 8;

    private static final Instant STARTS = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-06-01T10:00:00Z");

    @Test
    void anIndividualLessonHoldsExactlyOneStudent() {
        assertThat(lesson(LessonType.INDIVIDUAL, 1).capacity()).isEqualTo(1);

        assertThatThrownBy(() -> lesson(LessonType.INDIVIDUAL, 2))
                .isInstanceOf(InvalidLessonException.class);
    }

    /** The product says a group lesson may start with a single student, so one is a valid size. */
    @Test
    void aGroupLessonMayHoldFromOneStudentUpToTheConfiguredCap() {
        assertThat(lesson(LessonType.GROUP, 1).capacity()).isEqualTo(1);
        assertThat(lesson(LessonType.GROUP, MAX_GROUP).capacity()).isEqualTo(MAX_GROUP);
    }

    @Test
    void refusesAGroupLargerThanTheConfiguredCap() {
        assertThatThrownBy(() -> lesson(LessonType.GROUP, MAX_GROUP + 1))
                .isInstanceOf(InvalidLessonException.class)
                .hasMessageContaining(String.valueOf(MAX_GROUP));
    }

    @Test
    void refusesACapacityBelowOne() {
        assertThatThrownBy(() -> lesson(LessonType.GROUP, 0))
                .isInstanceOf(InvalidLessonException.class);
    }

    @Test
    void refusesALessonThatRunsPastMidnightInTheTeachersZone() {
        assertThatThrownBy(() -> Lesson.create(TEACHER, LessonType.INDIVIDUAL,
                new LessonPeriod(Instant.parse("2026-06-01T21:00:00Z"),
                        Instant.parse("2026-06-01T22:30:00Z")),
                1, null, false, MADRID, MAX_GROUP))
                .isInstanceOf(InvalidLessonException.class)
                .hasMessageContaining("midnight");
    }

    /** Blank notes become nothing at all, so "empty" has one representation rather than two. */
    @Test
    void storesBlankNotesAsNothing() {
        assertThat(Lesson.create(TEACHER, LessonType.INDIVIDUAL, new LessonPeriod(STARTS, ENDS), 1,
                "   ", false, MADRID, MAX_GROUP).notes()).isNull();
        assertThat(Lesson.create(TEACHER, LessonType.INDIVIDUAL, new LessonPeriod(STARTS, ENDS), 1,
                "  bring balls ", false, MADRID, MAX_GROUP).notes()).isEqualTo("bring balls");
    }

    @Test
    void isOpenBeforeItStartsAndCompletedOnceItHasEnded() {
        Lesson lesson = lesson(LessonType.INDIVIDUAL, 1);

        assertThat(lesson.statusAt(STARTS.minusSeconds(60))).isEqualTo(LessonStatus.OPEN);
        assertThat(lesson.statusAt(ENDS.minusSeconds(60))).isEqualTo(LessonStatus.OPEN);
        assertThat(lesson.statusAt(ENDS)).isEqualTo(LessonStatus.COMPLETED);
        assertThat(lesson.statusAt(ENDS.plusSeconds(60))).isEqualTo(LessonStatus.COMPLETED);
    }

    /** Nobody runs anything for this: the status follows from the clock at the moment of reading. */
    @Test
    void becomesCompletedWithoutAnybodyChangingIt() {
        Lesson lesson = lesson(LessonType.GROUP, 4);

        assertThat(lesson.statusAt(STARTS)).isEqualTo(LessonStatus.OPEN);
        assertThat(lesson.statusAt(ENDS.plusSeconds(1))).isEqualTo(LessonStatus.COMPLETED);
    }

    @Test
    void cancellingRecordsWhenItHappenedAndOutranksHavingFinished() {
        Instant cancelledAt = STARTS.minusSeconds(3600);
        Lesson cancelled = lesson(LessonType.INDIVIDUAL, 1).cancel(cancelledAt);

        assertThat(cancelled.isCancelled()).isTrue();
        assertThat(cancelled.cancelledAt()).isEqualTo(cancelledAt);
        assertThat(cancelled.statusAt(ENDS.plusSeconds(3600))).isEqualTo(LessonStatus.CANCELLED);
    }

    /** The original is untouched, which is what makes "a cancelled lesson is never reopened" a property of the type. */
    @Test
    void cancellingDoesNotChangeTheLessonItWasCalledOn() {
        Lesson lesson = lesson(LessonType.INDIVIDUAL, 1);

        lesson.cancel(STARTS.minusSeconds(3600));

        assertThat(lesson.isCancelled()).isFalse();
    }

    @Test
    void refusesToCancelTwice() {
        Lesson cancelled = lesson(LessonType.INDIVIDUAL, 1).cancel(STARTS.minusSeconds(3600));

        assertThatThrownBy(() -> cancelled.cancel(STARTS.minusSeconds(60)))
                .isInstanceOf(LessonAlreadyCancelledException.class);
    }

    @Test
    void refusesToCancelALessonThatAlreadyEnded() {
        assertThatThrownBy(() -> lesson(LessonType.INDIVIDUAL, 1).cancel(ENDS.plusSeconds(1)))
                .isInstanceOf(LessonAlreadyFinishedException.class);
    }

    /**
     * The teacher may cancel at any notice - what the window decides is only whether Fase 9 has
     * to warn anybody, and that is worked out from the two instants rather than stored.
     */
    @Test
    void knowsWhetherItWasCancelledAtShortNotice() {
        Instant aWeekBefore = STARTS.minus(java.time.Duration.ofDays(7));
        Instant theNightBefore = STARTS.minus(java.time.Duration.ofHours(10));

        assertThat(lesson(LessonType.INDIVIDUAL, 1).cancel(aWeekBefore).cancelledAtShortNotice()).isFalse();
        assertThat(lesson(LessonType.INDIVIDUAL, 1).cancel(theNightBefore).cancelledAtShortNotice()).isTrue();
    }

    /** Exactly 24 hours is enough notice: the rule is "less than", not "at most". */
    @Test
    void exactlyTheNoticePeriodIsNotShortNotice() {
        Instant exactly = STARTS.minus(Lesson.CANCELLATION_NOTICE);

        assertThat(lesson(LessonType.INDIVIDUAL, 1).cancel(exactly).cancelledAtShortNotice()).isFalse();
    }

    @Test
    void refusesALessonWithNoTeacher() {
        assertThatThrownBy(() -> Lesson.create(null, LessonType.INDIVIDUAL,
                new LessonPeriod(STARTS, ENDS), 1, null, false, MADRID, MAX_GROUP))
                .isInstanceOf(InvalidLessonException.class);
    }

    private static Lesson lesson(LessonType type, int capacity) {
        return Lesson.create(TEACHER, type, new LessonPeriod(STARTS, ENDS), capacity, null, false,
                MADRID, MAX_GROUP);
    }
}
