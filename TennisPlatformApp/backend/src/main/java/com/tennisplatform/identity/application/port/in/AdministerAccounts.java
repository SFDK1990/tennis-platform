package com.tennisplatform.identity.application.port.in;

import com.tennisplatform.shared.domain.ResultPage;

import java.util.Optional;
import java.util.UUID;

/**
 * What the administration console does to accounts. Which accounts it may touch is decided by
 * {@code administration}; this module only keeps the state machine and the sessions right.
 */
public interface AdministerAccounts {

    /**
     * Newest first. Every filter is optional (null). The email filter is a partial,
     * case-insensitive match: unlike the teacher, the administrator may already see every
     * account, so a partial search reveals nothing new (23-fase12-analisis-administracion.md).
     */
    ResultPage<AccountView> list(String role, String status, String emailContains, int page, int size);

    Optional<AccountView> byId(UUID id);

    /** Also ends every session of the account: its refresh tokens stop working at once. */
    AccountView disable(UUID id);

    /** Back to active, or to pending verification if the address was never confirmed. */
    AccountView reactivate(UUID id);
}
