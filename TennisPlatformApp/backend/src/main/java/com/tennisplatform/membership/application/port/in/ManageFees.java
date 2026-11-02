package com.tennisplatform.membership.application.port.in;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** The fees the teacher records, and how each month of each membership stands. */
public interface ManageFees {

    /**
     * The months of the teacher's memberships: of {@code month} only, or up to the current one
     * when null, plus any recorded ahead. {@code studentUserId} narrows them to one student.
     */
    List<FeeLineView> lines(UUID teacherUserId, YearMonth month, UUID studentUserId);

    /**
     * Records a month as paid or not charged. Without an amount, a paid month costs the group's
     * fee. The month has to be one of the membership's.
     */
    FeeView record(UUID teacherUserId, NewFee fee);

    FeeView voidFee(UUID teacherUserId, UUID feeId);

    int dueDay(UUID teacherUserId);

    int changeDueDay(UUID teacherUserId, int dueDay);

    record NewFee(UUID membershipId, YearMonth month, String kind, Integer amountCents, LocalDate paidOn,
                  String paymentMethod) {
    }
}
