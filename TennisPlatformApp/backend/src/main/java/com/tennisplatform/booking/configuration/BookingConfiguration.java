package com.tennisplatform.booking.configuration;

import com.tennisplatform.booking.application.port.in.BookLesson;
import com.tennisplatform.booking.application.port.in.CancelBooking;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.booking.application.port.in.MarkAttendance;
import com.tennisplatform.booking.application.port.in.WaitingList;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.application.service.BookLessonService;
import com.tennisplatform.booking.application.service.BookingsOfLessons;
import com.tennisplatform.booking.application.service.BookingsOfPasses;
import com.tennisplatform.booking.application.service.BookingsOfStudents;
import com.tennisplatform.booking.application.service.CancelBookingService;
import com.tennisplatform.booking.application.service.GetBookingsService;
import com.tennisplatform.booking.application.service.MarkAttendanceService;
import com.tennisplatform.booking.application.service.WaitingListPromotion;
import com.tennisplatform.booking.application.service.WaitingListService;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.pass.application.port.in.ChargePasses;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do.
 *
 * <p>The three beans at the end are this module's side of the dependency inversion:
 * {@code lesson}, {@code student} and {@code pass} ask for {@code LessonBookings},
 * {@code StudentBookings} and {@code PassBookings}, declared in their own spi packages, and these
 * are what they get. Declared by concrete type so that nothing
 * here refers to another module's spi as a type to call.
 */
@Configuration
public class BookingConfiguration {

    @Bean
    public BookLesson bookLesson(BookingRepository bookings, LockLesson lockLesson, GetLesson getLesson,
                                 QueryManagedStudent managedStudents, ChargePasses passes, Clock clock,
                                 ApplicationEventPublisher publisher) {
        return new BookLessonService(bookings, lockLesson, getLesson, managedStudents, passes, clock, publisher);
    }

    @Bean
    public CancelBooking cancelBooking(BookingRepository bookings, GetLesson getLesson, LockLesson lockLesson,
                                       WaitingListPromotion promotion, Clock clock,
                                       ApplicationEventPublisher publisher) {
        return new CancelBookingService(bookings, getLesson, lockLesson, promotion, clock, publisher);
    }

    @Bean
    public WaitingList waitingList(WaitingListRepository waiting, BookingRepository bookings, LockLesson lockLesson,
                                   GetLesson getLesson, QueryManagedStudent managedStudents, ChargePasses passes,
                                   Clock clock) {
        return new WaitingListService(waiting, bookings, lockLesson, getLesson, managedStudents, passes, clock);
    }

    @Bean
    public WaitingListPromotion waitingListPromotion(BookingRepository bookings, WaitingListRepository waiting,
                                                     QueryManagedStudent managedStudents, ChargePasses passes,
                                                     Clock clock, ApplicationEventPublisher publisher) {
        return new WaitingListPromotion(bookings, waiting, managedStudents, passes, clock, publisher);
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
    public BookingsOfLessons bookingsOfLessons(BookingRepository bookings, WaitingListRepository waiting,
                                               WaitingListPromotion promotion, ApplicationEventPublisher publisher) {
        return new BookingsOfLessons(bookings, waiting, promotion, publisher);
    }

    @Bean
    public BookingsOfStudents bookingsOfStudents(BookingRepository bookings, WaitingListRepository waiting,
                                                 LockLesson lockLesson, WaitingListPromotion promotion,
                                                 ApplicationEventPublisher publisher) {
        return new BookingsOfStudents(bookings, waiting, lockLesson, promotion, publisher);
    }

    @Bean
    public BookingsOfPasses bookingsOfPasses(BookingRepository bookings) {
        return new BookingsOfPasses(bookings);
    }
}
