package com.tennisplatform.pass.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What other modules and the student read about passes. */
public interface QueryPasses {

    /** The student's own passes, newest first. */
    List<PassView> ofStudent(UUID studentUserId);

    /**
     * The passes the teacher sold between two days, the first included and the last not, by the
     * day they were paid. Voided ones are left out: they were never sold (decision 6).
     */
    PassSales soldBetween(UUID teacherUserId, LocalDate from, LocalDate until);

    record PassSales(int count, long totalCents) {
    }
}
