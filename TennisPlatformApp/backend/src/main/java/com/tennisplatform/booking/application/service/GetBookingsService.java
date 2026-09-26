package com.tennisplatform.booking.application.service;

import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.BookingRepository.Slice;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class GetBookingsService implements GetBookings {

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
