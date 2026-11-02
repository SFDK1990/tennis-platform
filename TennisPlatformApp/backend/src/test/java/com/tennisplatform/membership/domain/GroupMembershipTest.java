package com.tennisplatform.membership.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The period of a membership, and the rules of groups and fees that need no database. */
class GroupMembershipTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    private final FixedGroup group = new FixedGroup(UUID.randomUUID(), UUID.randomUUID(), "Martes", 6_000, NOW);

    /** Decision 5: in until the start of next month in Madrid, which is 23:00 UTC on 30 November. */
    @Test
    void leavingEndsAtTheStartOfNextMonthOnTheTeachersClock() {
        GroupMembership leaving = GroupMembership.join(group, UUID.randomUUID(), NOW).leavingAfterThisMonth(NOW, MADRID);

        assertThat(leaving.leftAt()).isEqualTo(Instant.parse("2026-11-30T23:00:00Z"));
        assertThat(leaving.isOpen()).isFalse();
        assertThat(leaving.isInAt(NOW)).isTrue();
        assertThat(leaving.covers(Instant.parse("2026-11-30T22:00:00Z"))).isTrue();
        assertThat(leaving.covers(Instant.parse("2026-12-01T09:00:00Z"))).isFalse();
        assertThatThrownBy(() -> leaving.leavingAfterThisMonth(NOW, MADRID)).isInstanceOf(NotInGroupException.class);
    }

    @Test
    void endingAtOnceKeepsAnEarlierEndAndNeverGoesBeforeTheStart() {
        GroupMembership joined = GroupMembership.join(group, UUID.randomUUID(), NOW);
        GroupMembership leaving = joined.leavingAfterThisMonth(NOW, MADRID);

        assertThat(joined.endedAt(NOW.plusSeconds(60)).leftAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(joined.endedAt(NOW.minusSeconds(60)).leftAt()).isEqualTo(NOW);
        assertThat(leaving.endedAt(NOW).leftAt()).isEqualTo(NOW);
        assertThat(joined.endedAt(NOW).endedAt(NOW.plusSeconds(60)).leftAt()).isEqualTo(NOW);
    }

    @Test
    void aGroupNeedsANameOfSixtyCharactersAndAFeeWithinBounds() {
        assertThatThrownBy(() -> FixedGroup.create(UUID.randomUUID(), null, 1, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> FixedGroup.create(UUID.randomUUID(), "x".repeat(61), 1, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> FixedGroup.create(UUID.randomUUID(), "Martes", -1, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThat(group.changed(null, 7_000)).extracting(FixedGroup::name, FixedGroup::monthlyFeeCents)
                .containsExactly("Martes", 7_000);
        assertThat(group.changed("  Jueves ", null).name()).isEqualTo("Jueves");
    }

    @Test
    void aPaidMonthSaysWhenAndHowAndAVoidedOneStaysVoided() {
        UUID membership = UUID.randomUUID();
        YearMonth november = YearMonth.of(2026, 11);
        LocalDate day = LocalDate.of(2026, 11, 3);

        assertThatThrownBy(() -> MonthlyFee.paid(membership, november, 6_000, null, FeePaymentMethod.CASH, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> MonthlyFee.paid(membership, november, 6_000, day, null, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> MonthlyFee.paid(membership, november, -1, day, FeePaymentMethod.CASH, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> MonthlyFee.paid(membership, null, 1, day, FeePaymentMethod.CASH, NOW))
                .isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> MonthlyFee.waived(membership, null, NOW)).isInstanceOf(InvalidMembershipException.class);
        MonthlyFee voided = MonthlyFee.waived(membership, november, NOW).voided(NOW);
        assertThat(voided.voided(NOW.plusSeconds(1)).voidedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> FeePaymentMethod.parse("CARD")).isInstanceOf(InvalidMembershipException.class);
        assertThat(FeePaymentMethod.parse(null)).isNull();
        assertThatThrownBy(() -> FeeKind.parse(null)).isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> FeeKind.parse("FREE")).isInstanceOf(InvalidMembershipException.class);
        assertThatThrownBy(() -> new FeeSettings(0)).isInstanceOf(InvalidMembershipException.class);
    }
}
