package com.tennisplatform.availability.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One rule as it arrives from a client. The weekday is a name, validated when the command is
 * turned into a domain rule; the dates are optional and mean "always" when absent.
 */
public record WeeklyRuleCommand(String dayOfWeek, LocalTime startTime, LocalTime endTime,
                                LocalDate activeFrom, LocalDate activeUntil) {
}
