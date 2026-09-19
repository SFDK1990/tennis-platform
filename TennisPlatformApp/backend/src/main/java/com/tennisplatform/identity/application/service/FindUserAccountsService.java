package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FindUserAccountsService implements FindUserAccounts {

    private final UserRepository users;

    public FindUserAccountsService(UserRepository users) {
        this.users = users;
    }

    /**
     * Building the {@link EmailAddress} is what normalizes the address, and what rejects a
     * malformed one. The rejection is turned into an empty result rather than propagated: the
     * caller is searching, and "no account has that address" is the honest answer to a string
     * that could not be an address in the first place.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> byEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        EmailAddress address;
        try {
            address = new EmailAddress(email);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return users.findByEmail(address).map(UserSummary::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> byIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(List.copyOf(ids)).stream()
                .map(UserSummary::of)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }
}
