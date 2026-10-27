package com.tennisplatform.calendar.application.port.in;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * The teacher's month in numbers (38-fase25-analisis-resumen-mes.md): lessons given, seats,
 * attendance, active students and cancellations, next to the month before and split by level.
 */
public interface GetMonthSummary {

    /** {@code month} null means the current one; a month that has not started is refused. */
    MonthSummaryView forTeacher(UUID teacherUserId, YearMonth month);

    record MonthSummaryView(String month, String timezone, MonthFigures current, MonthFigures previous,
                            List<LevelFigures> levels) {

        public MonthSummaryView {
            levels = List.copyOf(levels);
        }
    }

    /** {@code levelId} null is the lessons open to everybody. */
    record LevelFigures(UUID levelId, MonthFigures current, MonthFigures previous) {
    }

    /**
     * What one month adds up to. Occupancy is {@code seatsTaken / seatsOffered}; attendance is
     * {@code attended / (attended + noShow)}, with {@code unmarked} the seats nobody took the
     * register of.
     */
    record MonthFigures(int lessonsGiven, int individualLessons, int groupLessons, int seatsOffered, int seatsTaken,
                        int attended, int noShow, int unmarked, int activeStudents, int cancelledLessons,
                        int cancelledAtShortNotice) {
    }
}
