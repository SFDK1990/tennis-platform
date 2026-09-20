package com.tennisplatform.availability.adapters.out.persistence;

import com.tennisplatform.availability.domain.WeeklyAvailabilityRule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "weekly_availability_rules")
class WeeklyAvailabilityRuleEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    /**
     * ISO-8601: 1 is Monday and 7 is Sunday, which is exactly {@code DayOfWeek#getValue}.
     *
     * <p>Stored as the number rather than the name to keep the column narrow and indexable, and
     * converted right here so no other layer ever sees it. That matters because three
     * conventions collide around this field - the API draft said 0 = Monday and PostgreSQL's
     * {@code EXTRACT(DOW)} says 0 = Sunday - and picking the wrong one does not fail, it moves
     * the schedule by a day.
     */
    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "active_from")
    private LocalDate activeFrom;

    @Column(name = "active_until")
    private LocalDate activeUntil;

    protected WeeklyAvailabilityRuleEntity() {
    }

    static WeeklyAvailabilityRuleEntity fromDomain(WeeklyAvailabilityRule rule) {
        WeeklyAvailabilityRuleEntity entity = new WeeklyAvailabilityRuleEntity();
        entity.id = rule.id();
        entity.teacherUserId = rule.teacherUserId();
        entity.dayOfWeek = (short) rule.dayOfWeek().getValue();
        entity.startTime = rule.startTime();
        entity.endTime = rule.endTime();
        entity.activeFrom = rule.activeFrom();
        entity.activeUntil = rule.activeUntil();
        return entity;
    }

    WeeklyAvailabilityRule toDomain() {
        return WeeklyAvailabilityRule.rehydrate(id, teacherUserId, DayOfWeek.of(dayOfWeek),
                startTime, endTime, activeFrom, activeUntil);
    }
}
