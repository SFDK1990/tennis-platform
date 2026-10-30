package com.tennisplatform.pass.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code booking}'s questions about paying for a seat (decision 5).
 *
 * <p>{@link #charge} has to run inside the booking's transaction, after the lesson's lock: it
 * locks the student's passes, so two bookings of the same student in two different lessons wait
 * for each other, and the second one counts the first one's lesson as spent.
 */
public interface ChargePasses {

    /**
     * The pass that pays for a lesson of {@code lessonType} starting at {@code lessonStartsAt}.
     * Nothing is written: the booking stores the pass, and that is what spends the lesson.
     */
    PassCharge charge(UUID teacherUserId, UUID studentUserId, String lessonType, Instant lessonStartsAt);

    /** Whether {@link #charge} would find a pass now, without locking anything (the waiting list). */
    boolean canPay(UUID teacherUserId, UUID studentUserId, String lessonType, Instant lessonStartsAt);

    /**
     * {@code required} false: the teacher has not switched passes on, and the seat is free.
     * Otherwise {@code passId} is the pass to book with, or null when the student has none left.
     */
    record PassCharge(boolean required, UUID passId) {

        public boolean refused() {
            return required && passId == null;
        }
    }
}
