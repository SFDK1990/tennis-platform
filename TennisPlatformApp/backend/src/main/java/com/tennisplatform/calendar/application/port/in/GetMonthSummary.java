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

    /**
     * {@code sales} and {@code previousSales} are the passes sold in each month (Fase 27), the
     * only money the app knows about. They have no level: a pass is not for one. {@code fees} and
     * {@code previousFees} are the monthly fees collected (Fase 28), by the day they were paid.
     */
    record MonthSummaryView(String month, String timezone, MonthFigures current, MonthFigures previous,
                            List<LevelFigures> levels, Sales sales, Sales previousSales, Fees fees,
                            Fees previousFees) {

        public MonthSummaryView {
            levels = List.copyOf(levels);
        }
    }

    /** Passes sold by the day they were paid, voided ones left out, and what they add up to in cents. */
    record Sales(int passesSold, long totalCents) {
    }

    /** Monthly fees paid on a day of the month, voided ones left out, and what they add up to in cents. */
    record Fees(int feesPaid, long totalCents) {
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
