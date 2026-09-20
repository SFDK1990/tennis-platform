package com.tennisplatform.availability.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ConfigureWeeklyAvailability {

    /**
     * Replaces the teacher's whole weekly configuration with what arrives.
     *
     * <p><strong>There is no merge.</strong> A rule the client does not send is a rule that
     * stops existing, including one whose {@code activeFrom} is still in the future. That is why
     * {@link GetAvailability} returns the active period of every rule: a client that reads,
     * edits and writes back must be able to send everything it read.
     *
     * <p>The whole set is validated before anything is written, so a rejected request leaves the
     * previous configuration exactly as it was.
     */
    List<WeeklyRuleView> replace(UUID teacherUserId, List<WeeklyRuleCommand> rules);
}
