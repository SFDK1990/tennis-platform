package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Attaches its lesson to each booking, reading all the lessons of a batch in one go. A page of
 * twenty bookings costs one read of lessons and one count, not twenty of each.
 */
class BookingViews {

    private final GetLesson getLesson;

    BookingViews(GetLesson getLesson) {
        this.getLesson = getLesson;
    }

    List<BookingView> of(List<Booking> bookings) {
        List<UUID> lessonIds = bookings.stream().map(Booking::lessonId).distinct().toList();
        Map<UUID, LessonView> lessons = getLesson.byIds(lessonIds).stream()
                .collect(Collectors.toMap(LessonView::id, Function.identity()));
        return bookings.stream()
                .map(booking -> BookingView.from(booking, lessons.get(booking.lessonId())))
                .toList();
    }

    BookingView of(Booking booking) {
        return of(List.of(booking)).get(0);
    }
}
