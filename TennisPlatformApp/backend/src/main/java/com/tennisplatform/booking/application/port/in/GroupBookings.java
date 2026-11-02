package com.tennisplatform.booking.application.port.in;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The seats of a fixed group (Fase 28), which {@code membership} asks for when a student joins or
 * leaves it, or when the teacher adds a series to the group.
 *
 * <p>Both run inside the caller's transaction and lock each lesson, always in the same order, so
 * the last seat is as safe as when a single seat is booked. Neither tells anybody lesson by lesson:
 * joining a group of sixteen weeks is one email, not sixteen, and {@code membership} sends it.
 */
public interface GroupBookings {

    /**
     * Books a seat in each lesson for the membership, or none at all: when any lesson has no seat
     * left, nothing is written and the start of each full lesson is returned. A lesson where the
     * student already holds a seat keeps that seat as it is.
     *
     * <p>The rest is checked as when booking a single seat, and refused with the same errors: the
     * teacher has to manage the student, the level has to allow it, and the student cannot be in
     * two lessons at the same time.
     */
    List<Instant> bookSeats(UUID teacherUserId, UUID studentUserId, UUID membershipId,
                            Collection<UUID> lessonIds);

    /**
     * Cancels the membership's seats in the lessons that start at or after {@code from}, without the
     * 24-hour window, and hands each freed seat to the waiting list. {@code byStudent} says who
     * decided: the student who left the group, or the teacher who took them out.
     */
    void cancelSeats(UUID membershipId, Instant from, boolean byStudent);
}
