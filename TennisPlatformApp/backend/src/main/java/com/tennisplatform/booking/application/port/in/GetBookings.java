package com.tennisplatform.booking.application.port.in;

import com.tennisplatform.shared.domain.ResultPage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Listing bookings, newest lesson first. {@code status} and {@code lessonId} are optional filters. */
public interface GetBookings {

    ResultPage<BookingView> forStudent(UUID studentUserId, String status, int page, int size);

    /** Every booking of the student, newest lesson first: what exporting their data hands over. */
    List<BookingView> allOfStudent(UUID studentUserId);

    /**
     * The student's bookings in any of these lessons, whatever their status, without the lesson
     * attached ({@code lesson} is null): the caller already holds the lessons and asked for them.
     */
    List<BookingView> ofStudentInLessons(UUID studentUserId, Collection<UUID> lessonIds);

    /**
     * The bookings of the teacher's lessons. Filtering by one lesson is what builds the attendance
     * screen, which needs precisely the booking ids of that lesson.
     */
    /**
     * The confirmed bookings of these lessons, without the lesson attached: what the monthly
     * summary counts seats and attendance from (Fase 25).
     */
    List<BookingView> confirmedInLessons(Collection<UUID> lessonIds);

    /** The students with a confirmed booking in any of these lessons: who a message to a group reaches (Fase 24). */
    Set<UUID> studentsWithASeatIn(Collection<UUID> lessonIds);

    /** Every booking of the platform, to resolve an incident. The filters are the teacher's. */
    ResultPage<BookingView> forAdministration(UUID lessonId, String status, int page, int size);

    /** {@code lessonId} and {@code studentUserId} narrow it down; the student's history is the second (Fase 23). */
    ResultPage<BookingView> forTeacher(UUID teacherUserId, UUID lessonId, UUID studentUserId, String status, int page,
                                       int size);
}
