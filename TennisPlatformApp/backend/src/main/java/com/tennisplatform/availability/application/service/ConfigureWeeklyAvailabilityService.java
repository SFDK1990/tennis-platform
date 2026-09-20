package com.tennisplatform.availability.application.service;

import com.tennisplatform.availability.application.port.in.ConfigureWeeklyAvailability;
import com.tennisplatform.availability.application.port.in.WeeklyRuleCommand;
import com.tennisplatform.availability.application.port.in.WeeklyRuleView;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.domain.OverlappingAvailabilityRulesException;
import com.tennisplatform.availability.domain.WeeklyAvailabilityRule;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigureWeeklyAvailabilityService implements ConfigureWeeklyAvailability {

    private final AvailabilityRuleRepository rules;
    private final TeacherSchedules schedules;

    public ConfigureWeeklyAvailabilityService(AvailabilityRuleRepository rules,
                                              GetTeacherProfile teacherProfile) {
        this.rules = rules;
        this.schedules = new TeacherSchedules(teacherProfile);
    }

    /**
     * Builds every rule, checks the whole set, and only then writes.
     *
     * <p>The order matters more than it looks: validating while saving would leave half a
     * schedule behind when the fourth rule of a form clashes with the first, and the teacher
     * would be left with a configuration they never asked for and did not see.
     */
    @Override
    @Transactional
    public List<WeeklyRuleView> replace(UUID teacherUserId, List<WeeklyRuleCommand> commands) {
        schedules.requireTheTeacher(teacherUserId);

        List<WeeklyAvailabilityRule> incoming = new ArrayList<>();
        for (WeeklyRuleCommand command : commands) {
            incoming.add(WeeklyAvailabilityRule.create(teacherUserId,
                    WeeklyAvailabilityRule.parseDayOfWeek(command.dayOfWeek()),
                    command.startTime(), command.endTime(),
                    command.activeFrom(), command.activeUntil()));
        }
        rejectOverlaps(incoming);

        return rules.replaceAllForTeacher(teacherUserId, incoming).stream()
                .map(WeeklyRuleView::from).toList();
    }

    /**
     * Quadratic on purpose. A weekly schedule is a handful of rows per weekday, so the set is
     * small enough that an index would cost more to build than it saves to scan, and naming the
     * offending pair is worth more to whoever has to fix the form.
     */
    private void rejectOverlaps(List<WeeklyAvailabilityRule> incoming) {
        for (int i = 0; i < incoming.size(); i++) {
            for (int j = i + 1; j < incoming.size(); j++) {
                WeeklyAvailabilityRule one = incoming.get(i);
                WeeklyAvailabilityRule other = incoming.get(j);
                if (one.conflictsWith(other)) {
                    throw new OverlappingAvailabilityRulesException(
                            "Two rules for " + one.dayOfWeek() + " overlap: "
                                    + one.startTime() + "-" + one.endTime() + " and "
                                    + other.startTime() + "-" + other.endTime());
                }
            }
        }
    }
}
