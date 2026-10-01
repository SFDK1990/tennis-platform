package com.tennisplatform.booking.application.service;

import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.BookingRepository.Slice;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GetBookingsService implements GetBookings {

    private static final int EXPORT_PAGE_SIZE = 100;

    private final BookingRepository bookings;
    private final BookingViews views;

    public GetBookingsService(BookingRepository bookings, GetLesson getLesson) {
        this.bookings = bookings;
        this.views = new BookingViews(getLesson);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<BookingView> forStudent(UUID studentUserId, String status, int page, int size) {
        Slice slice = bookings.findForStudent(studentUserId, BookingStatus.filter(status), page, size);
        return new ResultPage<BookingView>(views.of(slice.items()), page, size, slice.total());
    }

    /** Read in pages, so a student with years of history never costs one unbounded query. */
    @Override
    @Transactional(readOnly = true)
    public List<BookingView> allOfStudent(UUID studentUserId) {
        List<BookingView> all = new ArrayList<>();
        for (int page = 0; ; page++) {
            Slice slice = bookings.findForStudent(studentUserId, null, page, EXPORT_PAGE_SIZE);
            all.addAll(views.of(slice.items()));
            if (slice.items().size() < EXPORT_PAGE_SIZE || all.size() >= slice.total()) {
                return all;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingView> ofStudentInLessons(UUID studentUserId, Collection<UUID> lessonIds) {
        if (lessonIds.isEmpty()) {
            return List.of();
        }
        return bookings.findByStudentInLessons(studentUserId, lessonIds).stream()
                .map(booking -> BookingView.from(booking, null))
                .toList();
    }

    /**
     * Ownership is in the query itself: it only ever reads rows whose teacher is the caller, so a
     * {@code lessonId} of somebody else's lesson returns an empty page rather than their bookings.
     */
    @Override
    @Transactional(readOnly = true)
    public ResultPage<BookingView> forTeacher(UUID teacherUserId, UUID lessonId, String status, int page, int size) {
        Slice slice = bookings.findForTeacher(teacherUserId, lessonId, BookingStatus.filter(status),
                page, size);
        return new ResultPage<BookingView>(views.of(slice.items()), page, size, slice.total());
    }
}
