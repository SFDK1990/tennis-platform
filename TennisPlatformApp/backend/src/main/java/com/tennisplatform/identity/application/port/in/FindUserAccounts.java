package com.tennisplatform.identity.application.port.in;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Look-ups of accounts for the modules that own something attached to one.
 *
 * <p>It exists because {@code student} holds the managed-student relationship but not the email
 * address: the address belongs to the account, and reaching into identity's tables to join on
 * it is exactly what the module boundaries forbid. This is the supported way to ask.
 */
public interface FindUserAccounts {

    /**
     * The account with this exact address, if any.
     *
     * <p>Exact and complete, never a partial match: a teacher looking a student up to manage
     * them knows the address, and a partial search would let them enumerate who is registered
     * on the platform and read the names of people they have no relationship with, which
     * 02-arquitectura.md forbids (16-fase6-analisis-perfiles.md).
     *
     * <p>The address is normalized the same way registration normalizes it, so case and
     * padding cannot turn a hit into a miss. A malformed address is a miss, not an error: this
     * is a search, and the caller asked a question rather than made a claim.
     */
    Optional<UserSummary> byEmail(String email);

    /**
     * The accounts with these ids, keyed by id and skipping the ones that do not exist.
     *
     * <p>In bulk because the alternative is one query per row of a listing. The ids come from
     * the caller's own rows, so this cannot be used to sweep the user table.
     */
    Map<UUID, UserSummary> byIds(Collection<UUID> ids);
}
