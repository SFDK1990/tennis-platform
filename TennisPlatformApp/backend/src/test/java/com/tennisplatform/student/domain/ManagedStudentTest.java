package com.tennisplatform.student.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedStudentTest {

    private static final Instant MANAGED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Test
    void aNewRelationshipStartsManaged() {
        ManagedStudent relationship = take();

        assertThat(relationship.status()).isEqualTo(ManagedStatus.MANAGED);
        assertThat(relationship.isManaged()).isTrue();
        assertThat(relationship.managedAt()).isEqualTo(MANAGED_AT);
        assertThat(relationship.deactivatedAt()).isNull();
    }

    @Test
    void deactivatingRecordsWhenItHappened() {
        ManagedStudent relationship = take();

        relationship.deactivate(LATER);

        assertThat(relationship.status()).isEqualTo(ManagedStatus.INACTIVE);
        assertThat(relationship.isManaged()).isFalse();
        assertThat(relationship.deactivatedAt()).isEqualTo(LATER);
    }

    /**
     * Criterion 7 of Fase 6, the half that lives in the domain: a deactivated student stops
     * counting as managed, and taking them back reuses this row instead of starting a second
     * relationship. The unique index on the pair enforces the same thing in the schema.
     */
    @Test
    void takingAStudentBackReusesTheSameRelationship() {
        ManagedStudent relationship = take();
        UUID id = relationship.id();
        relationship.deactivate(LATER);

        relationship.reactivate(LATER);

        assertThat(relationship.id()).isEqualTo(id);
        assertThat(relationship.status()).isEqualTo(ManagedStatus.MANAGED);
        assertThat(relationship.deactivatedAt()).isNull();
    }

    /** "Managed since" has to mean the current spell, not the one that was ended. */
    @Test
    void takingAStudentBackMovesTheManagedSinceDate() {
        ManagedStudent relationship = take();
        relationship.deactivate(LATER);

        relationship.reactivate(LATER);

        assertThat(relationship.managedAt()).isEqualTo(LATER);
    }

    @Test
    void anAlreadyDeactivatedStudentCannotBeDeactivatedAgain() {
        ManagedStudent relationship = take();
        relationship.deactivate(LATER);

        assertThatThrownBy(() -> relationship.deactivate(LATER))
                .isInstanceOf(StudentAlreadyInactiveException.class);
    }

    @Test
    void anAlreadyManagedStudentCannotBeTakenOnTwice() {
        ManagedStudent relationship = take();

        assertThatThrownBy(() -> relationship.reactivate(LATER))
                .isInstanceOf(StudentAlreadyManagedException.class);
    }

    private ManagedStudent take() {
        return ManagedStudent.take(TEACHER_ID, STUDENT_ID, MANAGED_AT);
    }
}
