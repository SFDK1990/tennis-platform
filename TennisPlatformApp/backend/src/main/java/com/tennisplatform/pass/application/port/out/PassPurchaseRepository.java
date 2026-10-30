package com.tennisplatform.pass.application.port.out;

import com.tennisplatform.pass.domain.PassPurchase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassPurchaseRepository {

    Optional<PassPurchase> findById(UUID id);

    /**
     * {@code SELECT ... FOR UPDATE}: two copies of the same notice arriving at once are handled
     * one after the other, and the second finds the purchase already paid.
     */
    Optional<PassPurchase> lockByCheckoutSessionId(String checkoutSessionId);

    /** Newest first. */
    List<PassPurchase> findByStudent(UUID studentUserId);

    PassPurchase save(PassPurchase purchase);
}
