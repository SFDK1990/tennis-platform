package com.tennisplatform.calendar.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/** A range of days as each side sees it. The dates are read in the teacher's zone. */
public interface GetCalendar {

    /** Their resolved availability and every lesson of the range, cancelled ones included. */
    CalendarView forTeacher(UUID teacherUserId, LocalDate from, LocalDate to);

    /**
     * The lessons of the teachers who manage this student, each with the student's own booking
     * if there is one. Cancelled lessons only when the student had a booking in them.
     */
    CalendarView forStudent(UUID studentUserId, LocalDate from, LocalDate to);
}
