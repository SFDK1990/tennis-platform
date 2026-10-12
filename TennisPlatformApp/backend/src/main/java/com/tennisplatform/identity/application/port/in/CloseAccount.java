package com.tennisplatform.identity.application.port.in;

import java.util.UUID;

/**
 * The account half of a student deleting their account (30-fase19-analisis-cierre-mvp.md): it
 * checks the password, then leaves nothing in the account that identifies anybody and ends its
 * sessions. {@code student} calls it inside the transaction that also releases the student's
 * seats and empties their profile, so either all of it happens or none does.
 */
public interface CloseAccount {

    /**
     * @throws com.tennisplatform.identity.domain.CurrentPasswordIncorrectException if the password is not the account's
     * @throws com.tennisplatform.shared.domain.ForbiddenOperationException if the account is not a student's
     */
    void close(UUID userId, String password);
}
