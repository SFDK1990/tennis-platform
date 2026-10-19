package com.tennisplatform.notification.application.port.out;

/**
 * One email, ready to send.
 *
 * <p>{@code kind} names what it is for the metric and the log ({@code booking-confirmed-student}
 * and the like) and {@code reference} is the booking id: together they say which email failed
 * without writing anybody's address.
 *
 * @param calendar null when the email carries no calendar attachment
 */
public record Mail(String kind, String reference, String to, String subject, String body, Calendar calendar) {

    /** An iCalendar attachment: its method ({@code REQUEST} or {@code CANCEL}) and its text. */
    public record Calendar(String method, String content) {
    }
}
