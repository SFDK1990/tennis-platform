package com.tennisplatform.booking.configuration;

import com.tennisplatform.booking.application.port.in.BookLesson;
import com.tennisplatform.booking.application.port.in.CancelBooking;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.booking.application.port.in.MarkAttendance;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.service.BookLessonService;
import com.tennisplatform.booking.application.service.BookingsOfLessons;
import com.tennisplatform.booking.application.service.BookingsOfStudents;
import com.tennisplatform.booking.application.service.CancelBookingService;
import com.tennisplatform.booking.application.service.GetBookingsService;
import com.tennisplatform.booking.application.service.MarkAttendanceService;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do.
 *
 * <p>The two beans at the end are this module's side of the dependency inversion: {@code lesson}
 * and {@code student} ask for {@code LessonBookings} and {@code StudentBookings}, declared in
 * their own spi packages, and these are what they get. Declared by concrete type so that nothing
 * here refers to another module's spi as a type to call.
 */
@Configuration
public class BookingConfiguration {

    @Bean
    public BookLesson bookLesson(BookingRepository bookings, LockLesson lockLesson, GetLesson getLesson,
                                 QueryManagedStudent managedStudents, Clock clock) {
        return new BookLessonService(bookings, lockLesson, getLesson, managedStudents, clock);
    }

    @Bean
    public CancelBooking cancelBooking(BookingRepository bookings, GetLesson getLesson, Clock clock) {
        return new CancelBookingService(bookings, getLesson, clock);
    }

    @Bean
    public GetBookings getBookings(BookingRepository bookings, GetLesson getLesson) {
        return new GetBookingsService(bookings, getLesson);
    }

    @Bean
    public MarkAttendance markAttendance(BookingRepository bookings, GetLesson getLesson, Clock clock) {
        return new MarkAttendanceService(bookings, getLesson, clock);
    }

    @Bean
    public BookingsOfLessons bookingsOfLessons(BookingRepository bookings) {
        return new BookingsOfLessons(bookings);
    }

    @Bean
    public BookingsOfStudents bookingsOfStudents(BookingRepository bookings) {
        return new BookingsOfStudents(bookings);
    }
}
