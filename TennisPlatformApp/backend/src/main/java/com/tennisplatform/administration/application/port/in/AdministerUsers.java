package com.tennisplatform.administration.application.port.in;

import com.tennisplatform.identity.application.port.in.AccountView;
import com.tennisplatform.shared.domain.ResultPage;

import java.util.UUID;

/** The console's view of the accounts, and the one thing it changes about them: whether they are active. */
public interface AdministerUsers {

    /** Every filter optional; see {@code AdministerAccounts.list}. */
    ResultPage<AccountView> list(String role, String status, String emailContains, int page, int size);

    /**
     * Activates or disables a student account. Disabling ends their sessions and their
     * management, and cancels their upcoming bookings; activating does neither of the last two
     * back. Only students: disabling the only teacher would switch the platform off, and an
     * administrator disabling one (or themselves) could leave nobody to undo it.
     */
    AccountView changeStatus(UUID accountId, boolean active);
}
