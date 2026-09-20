package com.tennisplatform.availability.application.port.in;

import java.time.LocalDate;

/**
 * Reads the teacher's configuration as configured, rules and exceptions, without resolving them.
 *
 * <p>Separate from {@link QueryAvailability}, which answers the resolved question. The screen
 * that edits a schedule needs the rules it will send back; the module that validates a lesson
 * needs instants and must never see a rule. Serving both from one port would push the choice
 * onto every caller.
 */
public interface GetAvailability {

    /**
     * Every weekly rule, plus the exceptions between the two dates, both included.
     *
     * <p>The range is mandatory and capped: exceptions accumulate for ever, and an unbounded
     * read is one that gets slower every season until somebody notices.
     */
    AvailabilityView get(LocalDate from, LocalDate to);
}
