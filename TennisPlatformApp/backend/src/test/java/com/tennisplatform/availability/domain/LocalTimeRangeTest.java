package com.tennisplatform.availability.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalTimeRangeTest {

    /** Half-open: sharing an endpoint is not sharing time. */
    @Test
    void rangesThatOnlyTouchDoNotOverlap() {
        assertThat(range(9, 11).overlaps(range(11, 13))).isFalse();
        assertThat(range(9, 11).overlaps(range(10, 13))).isTrue();
    }

    @Test
    void unionMergesOverlappingAndAdjacentRangesAndLeavesGapsAlone() {
        assertThat(LocalTimeRange.union(List.of(range(9, 11), range(11, 13))))
                .containsExactly(range(9, 13));
        assertThat(LocalTimeRange.union(List.of(range(9, 12), range(10, 13))))
                .containsExactly(range(9, 13));
        assertThat(LocalTimeRange.union(List.of(range(16, 18), range(9, 11))))
                .containsExactly(range(9, 11), range(16, 18));
    }

    @Test
    void aCutInTheMiddleLeavesTwoPieces() {
        assertThat(LocalTimeRange.subtract(List.of(range(9, 13)), List.of(range(10, 11))))
                .containsExactly(range(9, 10), range(11, 13));
    }

    @Test
    void aCutThatCoversEverythingLeavesNothing() {
        assertThat(LocalTimeRange.subtract(List.of(range(9, 13)), List.of(range(8, 14)))).isEmpty();
    }

    @Test
    void aCutThatMissesChangesNothing() {
        assertThat(LocalTimeRange.subtract(List.of(range(9, 13)), List.of(range(14, 15))))
                .containsExactly(range(9, 13));
    }

    @Test
    void aCutThatOnlyTouchesAnEndChangesNothing() {
        assertThat(LocalTimeRange.subtract(List.of(range(9, 13)), List.of(range(13, 15))))
                .containsExactly(range(9, 13));
    }

    private static LocalTimeRange range(int startHour, int endHour) {
        return new LocalTimeRange(LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }
}
