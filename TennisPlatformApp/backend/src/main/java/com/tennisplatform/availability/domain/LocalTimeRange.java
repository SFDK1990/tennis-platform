package com.tennisplatform.availability.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A wall-clock interval within one day, closed on the left and open on the right.
 *
 * <p>The half-open reading is what makes 09:00-11:00 and 11:00-13:00 two adjacent rules rather
 * than two overlapping ones. Getting this wrong rejects a perfectly ordinary schedule, so it is
 * stated here rather than left to whoever writes the comparison.
 *
 * <p>No range can cross midnight: the schema already refuses {@code start >= end}, and a
 * midnight-crossing rule would need a second date to mean anything.
 */
public record LocalTimeRange(LocalTime start, LocalTime end) {

    public LocalTimeRange {
        if (start == null || end == null) {
            throw new InvalidAvailabilityException("An interval needs both a start and an end");
        }
        if (!start.isBefore(end)) {
            throw new InvalidAvailabilityException(
                    "The end time must be after the start time, and " + end + " is not after " + start);
        }
    }

    /** Sharing only an endpoint is not overlapping. */
    public boolean overlaps(LocalTimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Merges overlapping and adjacent ranges into the smallest equivalent set, in order. */
    public static List<LocalTimeRange> union(List<LocalTimeRange> ranges) {
        List<LocalTimeRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(LocalTimeRange::start).thenComparing(LocalTimeRange::end));

        List<LocalTimeRange> merged = new ArrayList<>();
        for (LocalTimeRange range : sorted) {
            if (merged.isEmpty()) {
                merged.add(range);
                continue;
            }
            LocalTimeRange last = merged.get(merged.size() - 1);
            // Adjacent ranges merge too: 09:00-11:00 plus 11:00-13:00 is one block of work, and
            // leaving them apart would later report a zero-length gap that does not exist.
            if (!range.start().isAfter(last.end())) {
                merged.set(merged.size() - 1,
                        new LocalTimeRange(last.start(), maxOf(last.end(), range.end())));
            } else {
                merged.add(range);
            }
        }
        return merged;
    }

    /** Everything in {@code base} that no range in {@code cuts} covers. */
    public static List<LocalTimeRange> subtract(List<LocalTimeRange> base, List<LocalTimeRange> cuts) {
        List<LocalTimeRange> remaining = new ArrayList<>(base);
        for (LocalTimeRange cut : union(cuts)) {
            List<LocalTimeRange> next = new ArrayList<>();
            for (LocalTimeRange range : remaining) {
                next.addAll(range.minus(cut));
            }
            remaining = next;
        }
        return remaining;
    }

    private List<LocalTimeRange> minus(LocalTimeRange cut) {
        if (!overlaps(cut)) {
            return List.of(this);
        }
        List<LocalTimeRange> pieces = new ArrayList<>(2);
        if (start.isBefore(cut.start())) {
            pieces.add(new LocalTimeRange(start, cut.start()));
        }
        if (cut.end().isBefore(end)) {
            pieces.add(new LocalTimeRange(cut.end(), end));
        }
        return pieces;
    }

    private static LocalTime maxOf(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }
}
