package com.tennisplatform.booking.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * The teacher records who came to a lesson.
 *
 * <p>All or nothing: if any entry cannot be applied, none is. Attendance recorded for half a
 * lesson is worse than none, because it looks complete.
 */
public interface MarkAttendance {

    List<BookingView> mark(UUID teacherUserId, UUID lessonId, List<Entry> entries);

    record Entry(UUID bookingId, String attendance) {
    }
}
