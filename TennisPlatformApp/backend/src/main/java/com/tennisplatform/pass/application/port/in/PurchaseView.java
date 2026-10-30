package com.tennisplatform.pass.application.port.in;

import com.tennisplatform.pass.domain.PassPurchase;
import com.tennisplatform.pass.domain.PurchaseStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * A purchase as the student sees it. {@code checkoutUrl} only while it can still be paid;
 * {@code passId} once it has been.
 */
public record PurchaseView(UUID id, UUID passTypeId, String name, String lessonType, int lessons, int priceCents,
                           Integer validityMonths, String status, String checkoutUrl, UUID passId,
                           Instant createdAt, Instant expiresAt, Instant paidAt) {

    public static PurchaseView from(PassPurchase purchase) {
        return new PurchaseView(purchase.id(), purchase.passTypeId(), purchase.name(),
                purchase.lessonType().name(), purchase.lessons(), purchase.priceCents(), purchase.validityMonths(),
                purchase.status().name(),
                purchase.status() == PurchaseStatus.PENDING ? purchase.checkoutUrl() : null,
                purchase.passId(), purchase.createdAt(), purchase.expiresAt(), purchase.paidAt());
    }
}
