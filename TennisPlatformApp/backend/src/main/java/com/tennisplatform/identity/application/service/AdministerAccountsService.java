package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.AccountView;
import com.tennisplatform.identity.application.port.in.AdministerAccounts;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.identity.domain.UserStatus;
import com.tennisplatform.shared.domain.ResultPage;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class AdministerAccountsService implements AdministerAccounts {

    private final UserRepository users;
    private final RefreshTokens refreshTokens;
    private final Clock clock;

    public AdministerAccountsService(UserRepository users, RefreshTokens refreshTokens, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<AccountView> list(String role, String status, String emailContains, int page, int size) {
        return users.findAll(
                        role == null ? null : Role.valueOf(role),
                        status == null ? null : UserStatus.valueOf(status),
                        emailContains == null || emailContains.isBlank() ? null : emailContains.trim(),
                        page, size)
                .map(AccountView::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountView> byId(UUID id) {
        return users.findById(id).map(AccountView::of);
    }

    /**
     * The access token already issued keeps working until it expires, 15 minutes at most:
     * checking the status on every request would cost a query per call to shorten a window
     * that is already short (23-fase12-analisis-administracion.md).
     */
    @Override
    @Transactional
    public AccountView disable(UUID id) {
        AccountView disabled = change(id, User::disable);
        refreshTokens.revokeAllForUser(id, clock.instant());
        return disabled;
    }

    @Override
    @Transactional
    public AccountView reactivate(UUID id) {
        return change(id, User::reactivate);
    }

    private AccountView change(UUID id, Consumer<User> transition) {
        User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("No account " + id));
        transition.accept(user);
        return AccountView.of(users.save(user));
    }
}
