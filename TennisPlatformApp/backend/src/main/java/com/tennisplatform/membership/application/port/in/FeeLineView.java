package com.tennisplatform.membership.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One month of one membership: whether it is paid, not charged, still to pay or overdue, and the
 * fee recorded for it when there is one.
 */
public record FeeLineView(UUID membershipId, UUID groupId, String groupName, UUID studentUserId, String month,
                          LocalDate dueOn, String status, int monthlyFeeCents, FeeView fee) {
}
