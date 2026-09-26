package com.tennisplatform.availability.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Turns weekly rules and date exceptions into concrete intervals of real time.
 *
 * <p>This is the whole point of the module having a domain. Resolving a schedule means applying
 * the active period of each rule, adding the extras, removing the blocks and converting wall
 * clock to instants with the teacher's zone. If {@code lesson} and {@code calendar} each did it
 * themselves there would be two implementations of one rule, and two implementations of one rule
 * diverge - the calendar would eventually draw a slot that creating a lesson rejects.
 *
 * <p>Pure: no Spring, no repository, no clock. It takes what it needs and returns a value, which
 * is what lets the daylight-saving cases be unit tests measured in milliseconds instead of
 * integration tests nobody wants to write twice.
 */
public final class AvailabilitySchedule {

    private final List<WeeklyAvailabilityRule> rules;
    private final List<AvailabilityOverride> exceptions;
    private final ZoneId zone;

    private AvailabilitySchedule(List<WeeklyAvailabilityRule> rules,
                                 List<AvailabilityOverride> exceptions, ZoneId zone) {
        this.rules = rules;
        this.exceptions = exceptions;
        this.zone = zone;
    }

    public static AvailabilitySchedule of(Collection<WeeklyAvailabilityRule> rules,
                                          Collection<AvailabilityOverride> exceptions,
                                          ZoneId zone) {
        if (zone == null) {
            throw new InvalidAvailabilityException("A schedule cannot be resolved without a time zone");
        }
        return new AvailabilitySchedule(List.copyOf(rules), List.copyOf(exceptions), zone);
    }

    /**
     * The availability between two dates, both included, as instants.
     *
     * <p>Intervals that meet across a date boundary are merged, so a caller never has to know
     * that the schedule is stored a day at a time.
     *
     * <p>Each day rescans the whole exception list rather than reading from a map built once,
     * which is quadratic on paper. It is left that way on purpose: {@link DateRange}
     * caps a range at {@code MAX_DAYS} and callers hand over only the exceptions of those same
     * days, so both sides of the product are bounded by the same two months. The scan it saves
     * is worth far less than the round trips that loaded the data.
     */
    public List<AvailabilityInterval> resolve(LocalDate from, LocalDate to) {
        List<AvailabilityInterval> intervals = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (LocalTimeRange range : availableHoursOn(date)) {
                toInterval(date, range).ifPresent(intervals::add);
            }
        }
        return merge(intervals);
    }

    /** Whether the teacher is available for the whole of {@code [from, to)}. */
    public boolean covers(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            return false;
        }
        return resolve(firstDayAround(from, zone), lastDayAround(to, zone)).stream()
                .anyMatch(interval -> interval.covers(from, to));
    }

    /**
     * The first local date whose rules can reach an instant, and the last one, with a day of
     * padding on each side.
     *
     * <p>Which local date an instant falls on depends on the offset in force, and an interval
     * that starts late on one day can be the one that covers early the next. Both the caller
     * choosing what to load and {@link #covers} deciding what to resolve need the same answer:
     * stated twice they would drift, and the schedule would be read over a window narrower than
     * the one it was resolved against.
     */
    public static LocalDate firstDayAround(Instant from, ZoneId zone) {
        return from.atZone(zone).toLocalDate().minusDays(1);
    }

    /** The far end of {@link #firstDayAround}. */
    public static LocalDate lastDayAround(Instant to, ZoneId zone) {
        return to.atZone(zone).toLocalDate().plusDays(1);
    }

    /** The wall-clock hours left on a date once the exceptions have had their say. */
    public List<LocalTimeRange> availableHoursOn(LocalDate date) {
        List<AvailabilityOverride> forDate = exceptions.stream()
                .filter(exception -> exception.date().equals(date))
                .toList();

        if (forDate.stream().anyMatch(AvailabilityOverride::blocksTheWholeDay)) {
            return List.of();
        }

        List<LocalTimeRange> available = new ArrayList<>();
        rules.stream().filter(rule -> rule.appliesOn(date)).map(WeeklyAvailabilityRule::hours)
                .forEach(available::add);
        forDate.stream().filter(exception -> exception.type() == AvailabilityOverrideType.EXTRA)
                .forEach(exception -> exception.hours().ifPresent(available::add));

        List<LocalTimeRange> blocks = forDate.stream()
                .filter(exception -> exception.type() == AvailabilityOverrideType.BLOCK)
                .map(AvailabilityOverride::hours)
                .flatMap(Optional::stream)
                .toList();

        return LocalTimeRange.subtract(LocalTimeRange.union(available), blocks);
    }

    /**
     * Wall clock to instants for one date.
     *
     * <p>Empty when the two ends land on the same instant, which is not hypothetical: on the day
     * the clocks go forward, a rule of 02:00-03:00 in a zone that skips exactly that hour has
     * both ends resolve to 03:00. Returning an empty optional keeps that day's read from failing
     * on an interval that genuinely has no duration - a read must not answer 500 because of the
     * calendar.
     */
    private Optional<AvailabilityInterval> toInterval(LocalDate date, LocalTimeRange range) {
        // LocalDateTime#atZone resolves the two ambiguous days for us: a time in a gap moves
        // forward by the length of the gap, and a time that happens twice takes the earlier
        // offset. That behaviour is the decision recorded in 18-fase7-analisis-availability.md,
        // and it is pinned by a test per transition rather than left to be discovered.
        Instant start = date.atTime(range.start()).atZone(zone).toInstant();
        Instant end = date.atTime(range.end()).atZone(zone).toInstant();
        return start.isBefore(end)
                ? Optional.of(new AvailabilityInterval(start, end))
                : Optional.empty();
    }

    private static List<AvailabilityInterval> merge(List<AvailabilityInterval> intervals) {
        List<AvailabilityInterval> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparing(AvailabilityInterval::startsAt)
                .thenComparing(AvailabilityInterval::endsAt));

        List<AvailabilityInterval> merged = new ArrayList<>();
        for (AvailabilityInterval interval : sorted) {
            if (merged.isEmpty()) {
                merged.add(interval);
                continue;
            }
            AvailabilityInterval last = merged.get(merged.size() - 1);
            if (last.meetsOrOverlaps(interval)) {
                Instant end = last.endsAt().isAfter(interval.endsAt()) ? last.endsAt() : interval.endsAt();
                merged.set(merged.size() - 1, new AvailabilityInterval(last.startsAt(), end));
            } else {
                merged.add(interval);
            }
        }
        return merged;
    }
}
