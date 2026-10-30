package com.tennisplatform.pass.application.port.in;

import java.util.List;
import java.util.UUID;

/** A student buys a pass in the app and pays it at the gateway (42-fase27.5-analisis-pago-online.md). */
public interface BuyPasses {

    /**
     * Starts the purchase and opens the gateway's page. The student has to be verified and managed
     * by the teacher of the type, as to book, and the teacher has to have switched on online
     * payment. The pass is not created here: it is created when the gateway confirms the payment.
     */
    PurchaseView start(UUID studentUserId, boolean emailVerified, UUID passTypeId);

    /** One of the student's own purchases. Somebody else's answers as one that does not exist. */
    PurchaseView ofStudent(UUID studentUserId, UUID purchaseId);

    /** All of the student's purchases, newest first: their data export carries them. */
    List<PurchaseView> allOf(UUID studentUserId);
}
