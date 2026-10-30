package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.domain.PassLessonType;
import com.tennisplatform.pass.domain.PassPurchase;
import com.tennisplatform.pass.domain.PurchaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pass_purchases")
class PassPurchaseEntity {

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

    @Column(name = "validity_months", updatable = false)
    private Integer validityMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status;

    @Column(name = "checkout_session_id", length = 255)
    private String checkoutSessionId;

    @Column(name = "checkout_url", length = 1024)
    private String checkoutUrl;

    @Column(name = "pass_id")
    private UUID passId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected PassPurchaseEntity() {
    }

    static PassPurchaseEntity fromDomain(PassPurchase purchase) {
        PassPurchaseEntity entity = new PassPurchaseEntity();
        entity.id = purchase.id();
        entity.teacherUserId = purchase.teacherUserId();
        entity.studentUserId = purchase.studentUserId();
        entity.passTypeId = purchase.passTypeId();
        entity.name = purchase.name();
        entity.lessonType = purchase.lessonType();
        entity.lessons = purchase.lessons();
        entity.priceCents = purchase.priceCents();
        entity.validityMonths = purchase.validityMonths();
        entity.status = purchase.status();
        entity.checkoutSessionId = purchase.checkoutSessionId();
        entity.checkoutUrl = purchase.checkoutUrl();
        entity.passId = purchase.passId();
        entity.createdAt = purchase.createdAt();
        entity.expiresAt = purchase.expiresAt();
        entity.paidAt = purchase.paidAt();
        return entity;
    }

    PassPurchase toDomain() {
        return new PassPurchase(id, teacherUserId, studentUserId, passTypeId, name, lessonType, lessons, priceCents,
                validityMonths, status, checkoutSessionId, checkoutUrl, passId, createdAt, expiresAt, paidAt);
    }
}
