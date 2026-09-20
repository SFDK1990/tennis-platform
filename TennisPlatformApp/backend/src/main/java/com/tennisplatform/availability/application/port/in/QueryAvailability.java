package com.tennisplatform.availability.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The resolved schedule, for the modules that consume it: {@code lesson} validates a class
 * against it and {@code calendar} draws it.
 *
 * <p>The name is not decoration. {@code ModuleBoundariesTest#calendarOnlyUsesQueryPorts} only
 * lets {@code calendar} depend on ports whose name begins with Get, Find or Query, which is the
 * mechanical form of "calendar reads other modules, it never changes them". The name the body of
 * the interface asks for - {@code IsIntervalAvailable} - would fail that rule.
 *
 * <p>It takes the teacher id although the MVP has exactly one teacher. Passing it now is the
 * difference between a later phase adding a {@code WHERE} and a later phase rewriting every
 * caller.
 */
public interface QueryAvailability {

    /** Whether the teacher is available for the whole of {@code [from, to)}. */
    boolean covers(UUID teacherUserId, Instant from, Instant to);

    /** The availability between two instants, merged, in chronological order. */
    List<AvailabilityIntervalView> intervals(UUID teacherUserId, Instant from, Instant to);
}
