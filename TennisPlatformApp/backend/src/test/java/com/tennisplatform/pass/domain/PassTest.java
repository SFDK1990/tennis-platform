package com.tennisplatform.pass.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The rules of a pass with literal dates (41-fase27-analisis-bonos.md, decisions 3 and 4). */
class PassTest {

    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final LocalDate PAID = LocalDate.parse("2026-10-15");

    private static PassType type(Integer months) {
        return new PassType(UUID.randomUUID(), TEACHER, "10 de grupo", PassLessonType.GROUP, 10, 12_000, months, false);
    }

    @Test
    void lastsItsMonthsFromTheDayItWasPaidUpToThatDayIncluded() {
        Pass pass = Pass.sell(type(3), STUDENT, PAID, PaymentMethod.CASH, NOW);

        assertThat(pass.validUntil()).isEqualTo(LocalDate.parse("2027-01-15"));
        assertThat(pass.coversDay(LocalDate.parse("2027-01-15"))).isTrue();
        assertThat(pass.coversDay(LocalDate.parse("2027-01-16"))).isFalse();
        assertThat(Pass.sell(type(null), STUDENT, PAID, PaymentMethod.CASH, NOW).coversDay(LocalDate.parse("2040-01-01")))
                .isTrue();
    }

    @Test
    void itsStatusComesFromTheDayAndTheLessonsSpent() {
        Pass pass = Pass.sell(type(1), STUDENT, PAID, PaymentMethod.BIZUM, NOW);

        assertThat(pass.statusOn(LocalDate.parse("2026-11-15"), 3)).isEqualTo(PassStatus.ACTIVE);
        assertThat(pass.statusOn(LocalDate.parse("2026-11-16"), 3)).isEqualTo(PassStatus.EXPIRED);
        assertThat(pass.statusOn(LocalDate.parse("2026-11-01"), 10)).isEqualTo(PassStatus.USED_UP);
        assertThat(pass.remaining(12)).isZero();
        assertThat(pass.voided(0, NOW).statusOn(LocalDate.parse("2026-11-01"), 0)).isEqualTo(PassStatus.VOIDED);
    }

    @Test
    void aVoidedPassPaysForNothingAndAUsedOneCannotBeVoided() {
        Pass pass = Pass.sell(type(null), STUDENT, PAID, PaymentMethod.TRANSFER, NOW);

        assertThat(pass.voided(0, NOW).coversDay(PAID)).isFalse();
        assertThat(pass.voided(0, NOW).voided(5, NOW).voidedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> pass.voided(1, NOW)).isInstanceOf(PassAlreadyUsedException.class);
    }

    @Test
    void theOneThatExpiresFirstIsSpentFirstAndOneThatNeverExpiresLast() {
        Pass never = new Pass(UUID.randomUUID(), TEACHER, STUDENT, UUID.randomUUID(), "a", PassLessonType.GROUP, 1, 0,
                PAID, PaymentMethod.CASH, null, NOW, null);
        Pass late = new Pass(UUID.randomUUID(), TEACHER, STUDENT, UUID.randomUUID(), "b", PassLessonType.GROUP, 1, 0,
                PAID, PaymentMethod.CASH, LocalDate.parse("2027-06-01"), NOW, null);
        Pass soonNewer = new Pass(UUID.randomUUID(), TEACHER, STUDENT, UUID.randomUUID(), "c", PassLessonType.GROUP, 1,
                0, PAID, PaymentMethod.CASH, LocalDate.parse("2027-01-01"), NOW.plusSeconds(60), null);
        Pass soonOlder = new Pass(UUID.randomUUID(), TEACHER, STUDENT, UUID.randomUUID(), "d", PassLessonType.GROUP, 1,
                0, PAID, PaymentMethod.CASH, LocalDate.parse("2027-01-01"), NOW, null);

        assertThat(List.of(never, late, soonNewer, soonOlder).stream().sorted(Pass.SPENDING_ORDER).toList())
                .containsExactly(soonOlder, soonNewer, late, never);
    }

    @Test
    void aRetiredTypeIsNotSoldAndAPassNeedsWhenAndHowItWasPaid() {
        PassType retired = type(null).changed(null, null, null, null, null, false, true);

        assertThatThrownBy(() -> Pass.sell(retired, STUDENT, PAID, PaymentMethod.CASH, NOW))
                .isInstanceOf(PassTypeRetiredException.class);
        assertThatThrownBy(() -> Pass.sell(type(null), STUDENT, null, PaymentMethod.CASH, NOW))
                .isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> Pass.sell(type(null), STUDENT, PAID, null, NOW))
                .isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> PaymentMethod.parse(null)).isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> PassLessonType.parse(null)).isInstanceOf(InvalidPassException.class);
        assertThat(PaymentMethod.parse(" bizum ")).isEqualTo(PaymentMethod.BIZUM);
    }

    @Test
    void aTypeKeepsItsValuesInRange() {
        assertThatThrownBy(() -> new PassType(null, TEACHER, null, PassLessonType.GROUP, 1, 0, null, false))
                .isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> new PassType(null, TEACHER, "x".repeat(61), PassLessonType.GROUP, 1, 0, null, false))
                .isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> new PassType(null, TEACHER, "a", null, 1, 0, null, false))
                .isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> new PassType(null, TEACHER, "a", PassLessonType.GROUP, 201, 0, null, false))
                .isInstanceOf(InvalidPassException.class);
        assertThatThrownBy(() -> new PassType(null, TEACHER, "a", PassLessonType.GROUP, 1, 0, 0, false))
                .isInstanceOf(InvalidPassException.class);
        assertThat(type(3).changed("Nuevo", "INDIVIDUAL", 5, 100, null, true, null))
                .extracting(PassType::name, PassType::lessonType, PassType::validityMonths, PassType::retired)
                .containsExactly("Nuevo", PassLessonType.INDIVIDUAL, null, false);
    }
}
