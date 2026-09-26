package com.tennisplatform.booking.application.port.in;

import com.tennisplatform.shared.domain.ResultPage;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Listing bookings, newest lesson first. {@code status} and {@code lessonId} are optional filters. */
public interface GetBookings {

    ResultPage<BookingView> forStudent(UUID studentUserId, String status, int page, int size);

    /**
     * The bookings of the teacher's lessons. Filtering by one lesson is what builds the attendance
     * screen, which needs precisely the booking ids of that lesson.
     */
    /**
     * The student's bookings in any of these lessons, whatever their status, without the lesson
     * attached ({@code lesson} is null): the caller already holds the lessons and asked for them.
     */
    List<BookingView> ofStudentInLessons(UUID studentUserId, Collection<UUID> lessonIds);

    ResultPage<BookingView> forTeacher(UUID teacherUserId, UUID lessonId, String status, int page, int size);
}
