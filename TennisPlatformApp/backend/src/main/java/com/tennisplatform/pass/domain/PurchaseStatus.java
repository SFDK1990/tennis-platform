package com.tennisplatform.pass.domain;

/**
 * Where a purchase stands. {@code PENDING} until the payment gateway says how it ended: paid, or
 * abandoned and expired. Only the gateway's signed notice moves it (42-fase27.5, decision 3).
 */
public enum PurchaseStatus {
    PENDING,
    PAID,
    EXPIRED
}
