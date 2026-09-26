package com.tennisplatform.booking.adapters.in.web;

import com.tennisplatform.booking.application.port.in.BookingPage;
import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.lesson.application.port.in.LessonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The wire shapes of this module, grouped like {@code LessonDtos}. */
final class BookingDtos {

    private BookingDtos() {
    }

    record BookingResponse(UUID id, UUID lessonId, UUID studentUserId, String status,
                           String attendance, Instant bookedAt, Instant cancelledAt,
                           LessonSummary lesson) {

        static BookingResponse from(BookingView booking) {
            return new BookingResponse(booking.id(), booking.lessonId(), booking.studentUserId(),
                    booking.status(), booking.attendance(), booking.bookedAt(), booking.cancelledAt(),
                    LessonSummary.from(booking.lesson()));
        }
    }

    /**
     * The lesson as it travels inside a booking.
     *
     * <p>Without {@code notes}, for everybody. {@code lesson} only shows them to the teacher who
     * owns the lesson, and deciding that again here would mean a second copy of the rule; a
     * booking screen does not need the teacher's private shorthand, and whoever does can read the
     * lesson itself.
     */
    record LessonSummary(UUID id, UUID teacherUserId, String type, Instant startsAt, Instant endsAt,
                         int capacity, int bookedCount, String status) {

        static LessonSummary from(LessonView lesson) {
            if (lesson == null) {
                return null;
            }
            return new LessonSummary(lesson.id(), lesson.teacherUserId(), lesson.type(),
                    lesson.startsAt(), lesson.endsAt(), lesson.capacity(), lesson.bookedCount(),
                    lesson.status());
        }
    }

    record BookingPageResponse(List<BookingResponse> items, int page, int size, long totalItems) {

        static BookingPageResponse from(BookingPage page) {
            return new BookingPageResponse(page.items().stream().map(BookingResponse::from).toList(),
                    page.page(), page.size(), page.totalItems());
        }
    }

    record MarkAttendanceRequest(@NotEmpty List<@Valid @NotNull Entry> entries) {

        record Entry(@NotNull UUID bookingId, @NotNull String status) {
        }
    }
}
