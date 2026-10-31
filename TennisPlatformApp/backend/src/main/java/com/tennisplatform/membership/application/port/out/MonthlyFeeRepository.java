package com.tennisplatform.membership.application.port.out;

import com.tennisplatform.membership.domain.MonthlyFee;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyFeeRepository {

    /** Translates a second live fee of the same month into {@code FeeAlreadyRecordedException}. */
    MonthlyFee save(MonthlyFee fee);

    Optional<MonthlyFee> findById(UUID id);

    /** The fees of these memberships that were not voided. */
    List<MonthlyFee> findLiveOf(Collection<UUID> membershipIds);

    /** The teacher's fees paid on a day in {@code [from, until)}, voided ones left out. */
    List<MonthlyFee> findPaidBetween(UUID teacherUserId, LocalDate from, LocalDate until);
}
