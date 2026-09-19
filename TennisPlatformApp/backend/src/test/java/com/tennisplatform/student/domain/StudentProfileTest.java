package com.tennisplatform.student.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentProfileTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Test
    void aProfileNeedsAFullName() {
        assertThatThrownBy(() -> profileNamed("   "))
                .isInstanceOf(InvalidStudentProfileException.class)
                .hasMessageContaining("full name is required");
    }

    @Test
    void trimsTheValuesItStores() {
        StudentProfile profile = StudentProfile.create(STUDENT_ID, "  Ana Ruiz  ", " 600123123 ",
                null, null, NOW);

        assertThat(profile.fullName()).isEqualTo("Ana Ruiz");
        assertThat(profile.phone()).isEqualTo("600123123");
    }

    @Test
    void anOptionalFieldLeftOutStaysNull() {
        StudentProfile profile = profileNamed("Ana Ruiz");

        assertThat(profile.phone()).isNull();
        assertThat(profile.nationalId()).isNull();
        assertThat(profile.address()).isNull();
    }

    /** A partial update is the whole point of PATCH: what is not sent must survive. */
    @Test
    void anUpdateOnlyTouchesTheFieldsItCarries() {
        StudentProfile profile = StudentProfile.create(STUDENT_ID, "Ana Ruiz", "600123123",
                "12345678Z", "Calle Mayor 1", NOW);

        profile.update(null, "600999888", null, null);

        assertThat(profile.phone()).isEqualTo("600999888");
        assertThat(profile.fullName()).isEqualTo("Ana Ruiz");
        assertThat(profile.nationalId()).isEqualTo("12345678Z");
        assertThat(profile.address()).isEqualTo("Calle Mayor 1");
    }

    /**
     * An empty string is how a nullable field is emptied on purpose. Without it there would be
     * no way to withdraw an address once given, since null already means "not submitted".
     */
    @Test
    void anEmptyStringClearsAnOptionalField() {
        StudentProfile profile = StudentProfile.create(STUDENT_ID, "Ana Ruiz", "600123123",
                "12345678Z", "Calle Mayor 1", NOW);

        profile.update(null, "", "", "");

        assertThat(profile.phone()).isNull();
        assertThat(profile.nationalId()).isNull();
        assertThat(profile.address()).isNull();
    }

    @Test
    void theNameCannotBeClearedTheSameWay() {
        StudentProfile profile = profileNamed("Ana Ruiz");

        assertThatThrownBy(() -> profile.update("", null, null, null))
                .isInstanceOf(InvalidStudentProfileException.class);
        assertThat(profile.fullName()).isEqualTo("Ana Ruiz");
    }

    @Test
    void rejectsValuesLongerThanTheirColumn() {
        assertThatThrownBy(() -> StudentProfile.create(STUDENT_ID, "Ana Ruiz", null,
                "x".repeat(31), null, NOW))
                .isInstanceOf(InvalidStudentProfileException.class)
                .hasMessageContaining("national id");
    }

    /** The message may name the field; it must never quote restricted personal data. */
    @Test
    void theRejectionDoesNotEchoThePersonalData() {
        String nationalId = "x".repeat(31);

        assertThatThrownBy(() -> StudentProfile.create(STUDENT_ID, "Ana Ruiz", null, nationalId,
                null, NOW))
                .hasMessageNotContaining(nationalId);
    }

    private StudentProfile profileNamed(String fullName) {
        return StudentProfile.create(STUDENT_ID, fullName, null, null, null, NOW);
    }
}
