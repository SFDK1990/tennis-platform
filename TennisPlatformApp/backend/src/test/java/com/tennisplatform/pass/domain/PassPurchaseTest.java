package com.tennisplatform.pass.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A purchase in the app and the pass it becomes (42-fase27.5-analisis-pago-online.md, decision 3). */
class PassPurchaseTest {

    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(3600);

    private static PassType type(int priceCents, boolean retired) {
        return new PassType(UUID.randomUUID(), TEACHER, "10 de grupo", PassLessonType.GROUP, 10, priceCents, 3, retired);
    }

    @Test
    void copiesItsTypeAndStartsPending() {
        PassType type = type(12_000, false);

        PassPurchase purchase = PassPurchase.start(type, STUDENT, NOW, LATER);

        assertThat(purchase.status()).isEqualTo(PurchaseStatus.PENDING);
        assertThat(purchase.priceCents()).isEqualTo(12_000);
        assertThat(purchase.lessons()).isEqualTo(10);
        assertThat(purchase.validityMonths()).isEqualTo(3);
        assertThat(purchase.passTypeId()).isEqualTo(type.id());
        assertThat(purchase.expiresAt()).isEqualTo(LATER);
    }

    @Test
    void aRetiredTypeOrOneUnderFiftyCentsIsNotSold() {
        assertThatThrownBy(() -> PassPurchase.start(type(12_000, true), STUDENT, NOW, LATER))
                .isInstanceOf(PassTypeRetiredException.class);
        assertThatThrownBy(() -> PassPurchase.start(type(49, false), STUDENT, NOW, LATER))
                .isInstanceOf(PriceTooLowToPayOnlineException.class);
        assertThat(PassPurchase.start(type(50, false), STUDENT, NOW, LATER).priceCents()).isEqualTo(50);
    }

    @Test
    void onlyTheExactAmountInEurosCountsAsCharged() {
        PassPurchase purchase = PassPurchase.start(type(12_000, false), STUDENT, NOW, LATER);

        assertThat(purchase.charged(12_000, "eur")).isTrue();
        assertThat(purchase.charged(12_000, "EUR")).isTrue();
        assertThat(purchase.charged(11_999, "eur")).isFalse();
        assertThat(purchase.charged(12_000, "usd")).isFalse();
        assertThat(purchase.charged(12_000, null)).isFalse();
    }

    @Test
    void aPaidPurchaseStaysPaidAndAPaymentAfterExpiringStillCounts() {
        PassPurchase pending = PassPurchase.start(type(12_000, false), STUDENT, NOW, LATER);
        UUID pass = UUID.randomUUID();

        PassPurchase paid = pending.paid(pass, LATER);
        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.passId()).isEqualTo(pass);
        assertThat(paid.paidAt()).isEqualTo(LATER);
        assertThat(paid.expired()).isSameAs(paid);

        PassPurchase expired = pending.expired();
        assertThat(expired.status()).isEqualTo(PurchaseStatus.EXPIRED);
        assertThat(expired.paid(pass, LATER).isPaid()).isTrue();
    }

    @Test
    void thePassItBecomesIsPaidOnlineAndLastsFromTheDayOfThePayment() {
        PassPurchase purchase = PassPurchase.start(type(12_000, false), STUDENT, NOW, LATER);

        Pass pass = Pass.boughtOnline(purchase, LocalDate.parse("2026-11-10"), LATER);

        assertThat(pass.paymentMethod()).isEqualTo(PaymentMethod.ONLINE);
        assertThat(pass.paidOn()).isEqualTo(LocalDate.parse("2026-11-10"));
        assertThat(pass.validUntil()).isEqualTo(LocalDate.parse("2027-02-10"));
        assertThat(pass.priceCents()).isEqualTo(12_000);
        assertThat(pass.studentUserId()).isEqualTo(STUDENT);
        assertThat(pass.teacherUserId()).isEqualTo(TEACHER);
    }

    @Test
    void theTeacherCannotRecordAPassAsPaidOnline() {
        assertThatThrownBy(() -> PaymentMethod.parse("ONLINE")).isInstanceOf(InvalidPassException.class);
        assertThat(PaymentMethod.parse("bizum")).isEqualTo(PaymentMethod.BIZUM);
    }

    @Test
    void bothSwitchesAreOffAndEachChangesAlone() {
        PassSettings settings = PassSettings.DEFAULTS;
        assertThat(settings).isEqualTo(new PassSettings(false, false));

        assertThat(settings.changed(null, true)).isEqualTo(new PassSettings(false, true));
        assertThat(settings.changed(true, null)).isEqualTo(new PassSettings(true, false));
        assertThat(new PassSettings(true, true).changed(null, null)).isEqualTo(new PassSettings(true, true));
    }
}
