package com.tennisplatform.booking.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Decision 4 of 34-fase21-analisis-lista-espera.md, at its boundary. */
class WaitingListEntryTest {

    private static final Instant STARTS = Instant.parse("2026-11-03T09:00:00Z");

    @Test
    void theListIsOpenUntilExactly24HoursBeforeTheLesson() {
        assertThat(WaitingListEntry.isOpen(STARTS, STARTS.minus(Duration.ofHours(25)))).isTrue();
        assertThat(WaitingListEntry.isOpen(STARTS, STARTS.minus(Duration.ofHours(24)))).isTrue();
    }

    @Test
    void itClosesOneSecondLater() {
        assertThat(WaitingListEntry.isOpen(STARTS, STARTS.minus(Duration.ofHours(24)).plusSeconds(1))).isFalse();
        assertThat(WaitingListEntry.isOpen(STARTS, STARTS.plusSeconds(1))).isFalse();
    }
}
