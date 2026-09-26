package com.tennisplatform.lesson.application.port.in;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Reading lessons.
 *
 * <p>A single lesson is readable by any authenticated caller - a student has to be able to see
 * what they are signing up for, and a lesson holds nobody's personal data. The listing is the
 * teacher's own diary and is scoped to them.
 */
public interface GetLesson {

    LessonView byId(UUID id);

    /**
     * Several lessons at once, in no particular order; ids that do not exist are left out. For
     * callers that hold a page of references - bookings - and would otherwise read them one by
     * one.
     */
    List<LessonView> byIds(Collection<UUID> ids);

    /**
     * The teacher's lessons whose time touches the given range of dates, both included.
     *
     * <p>The range is interpreted in the teacher's own zone, because a day is a local idea: "the
     * lessons of the 3rd" means the 3rd where the teacher lives.
     */
    List<LessonView> forTeacherBetween(UUID teacherUserId, LocalDate from, LocalDate to);
}
