package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.domain.FeeKind;
import com.tennisplatform.membership.domain.FeePaymentMethod;
import com.tennisplatform.membership.domain.MonthlyFee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** The month is stored as its first day, which is what the CHECK on the column asks for. */
@Entity
@Table(name = "monthly_fees")
class MonthlyFeeEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "membership_id", nullable = false, updatable = false)
    private UUID membershipId;

    @Column(nullable = false, updatable = false)
    private LocalDate month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false, length = 10)
    private FeeKind kind;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private int amountCents;

    @Column(name = "paid_on", updatable = false)
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", updatable = false, length = 20)
    private FeePaymentMethod paymentMethod;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    protected MonthlyFeeEntity() {
    }

    static MonthlyFeeEntity fromDomain(MonthlyFee fee) {
        MonthlyFeeEntity entity = new MonthlyFeeEntity();
        entity.id = fee.id();
        entity.membershipId = fee.membershipId();
        entity.month = fee.month().atDay(1);
        entity.kind = fee.kind();
        entity.amountCents = fee.amountCents();
        entity.paidOn = fee.paidOn();
        entity.paymentMethod = fee.paymentMethod();
        entity.recordedAt = fee.recordedAt();
        entity.voidedAt = fee.voidedAt();
        return entity;
    }

    MonthlyFee toDomain() {
        return new MonthlyFee(id, membershipId, YearMonth.from(month), kind, amountCents, paidOn, paymentMethod,
                recordedAt, voidedAt);
    }
}
