package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.domain.Pass;
import com.tennisplatform.pass.domain.PassLessonType;
import com.tennisplatform.pass.domain.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "passes")
class PassEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    @Column(name = "pass_type_id", nullable = false, updatable = false)
    private UUID passTypeId;

    @Column(nullable = false, updatable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, updatable = false, length = 20)
    private PassLessonType lessonType;

    @Column(nullable = false, updatable = false)
    private int lessons;

    @Column(name = "price_cents", nullable = false, updatable = false)
    private int priceCents;

    @Column(name = "paid_on", nullable = false, updatable = false)
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, updatable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "valid_until", updatable = false)
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    protected PassEntity() {
    }

    static PassEntity fromDomain(Pass pass) {
        PassEntity entity = new PassEntity();
        entity.id = pass.id();
        entity.teacherUserId = pass.teacherUserId();
        entity.studentUserId = pass.studentUserId();
        entity.passTypeId = pass.passTypeId();
        entity.name = pass.name();
        entity.lessonType = pass.lessonType();
        entity.lessons = pass.lessons();
        entity.priceCents = pass.priceCents();
        entity.paidOn = pass.paidOn();
        entity.paymentMethod = pass.paymentMethod();
        entity.validUntil = pass.validUntil();
        entity.createdAt = pass.createdAt();
        entity.voidedAt = pass.voidedAt();
        return entity;
    }

    Pass toDomain() {
        return new Pass(id, teacherUserId, studentUserId, passTypeId, name, lessonType, lessons, priceCents, paidOn,
                paymentMethod, validUntil, createdAt, voidedAt);
    }
}
