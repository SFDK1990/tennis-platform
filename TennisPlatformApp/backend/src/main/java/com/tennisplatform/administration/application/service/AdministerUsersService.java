package com.tennisplatform.administration.application.service;

import com.tennisplatform.administration.application.port.in.AdministerUsers;
import com.tennisplatform.administration.domain.AccountNotFoundException;
import com.tennisplatform.identity.application.port.in.AccountView;
import com.tennisplatform.identity.application.port.in.AdministerAccounts;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.student.application.port.in.ManageStudent;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class AdministerUsersService implements AdministerUsers {

    public static final String ADMIN_TARGET_NOT_ALLOWED = "ADMIN_TARGET_NOT_ALLOWED";

    private final AdministerAccounts accounts;
    private final ManageStudent students;

    public AdministerUsersService(AdministerAccounts accounts, ManageStudent students) {
        this.accounts = accounts;
        this.students = students;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<AccountView> list(String role, String status, String emailContains, int page, int size) {
        return accounts.list(role, status, emailContains, page, size);
    }

    /**
     * One transaction for the account, the management and the bookings: a student is never
     * left disabled while still holding seats, nor with seats freed but the account still open.
     */
    @Override
    @Transactional
    public AccountView changeStatus(UUID accountId, boolean active) {
        AccountView account = accounts.byId(accountId)
                .orElseThrow(() -> new AccountNotFoundException("No account with this id"));
        if (!account.isStudent()) {
            throw new ForbiddenOperationException(ADMIN_TARGET_NOT_ALLOWED,
                    "The console only changes the status of student accounts");
        }
        if (active) {
            return accounts.reactivate(accountId);
        }
        AccountView disabled = accounts.disable(accountId);
        students.releaseDisabledAccount(accountId);
        return disabled;
    }
}
