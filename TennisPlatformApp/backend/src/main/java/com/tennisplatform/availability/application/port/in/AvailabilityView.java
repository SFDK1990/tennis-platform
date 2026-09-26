package com.tennisplatform.availability.application.port.in;

import java.util.List;

/**
 * The teacher's configuration: every weekly rule, and the exceptions falling inside the range
 * that was asked for.
 *
 * <p>The rules come whole and the exceptions come filtered on purpose. Rules are a small bounded
 * set - a handful per weekday - and clipping them by date would only make the client reassemble
 * them. Exceptions accumulate for ever, so they need the range.
 */
public record AvailabilityView(List<WeeklyRuleView> weeklyRules,
                               List<AvailabilityOverrideView> exceptions) {

    /**
     * Copies both lists on the way in, like {@code ResultPage} does. A record gives
     * away its components by reference, which makes "immutable" true of the reference and not
     * of what it points at.
     */
    public AvailabilityView {
        weeklyRules = List.copyOf(weeklyRules);
        exceptions = List.copyOf(exceptions);
    }
}
