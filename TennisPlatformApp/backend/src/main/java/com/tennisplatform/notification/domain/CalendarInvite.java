package com.tennisplatform.notification.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * One lesson as an iCalendar event (RFC 5545), attached to the booking and cancellation emails.
 *
 * <p>Written by hand because it is a dozen lines. What goes wrong without anyone noticing is the
 * text escaping, the folding of long lines at 75 octets and the CRLF line ends: a calendar that
 * meets one it does not like drops the event and says nothing.
 *
 * <p>The booking id is the UID of both the invitation and the cancellation, which is what lets
 * the student's calendar remove the event it added instead of keeping it next to the email
 * that says the lesson is off. The cancellation carries a higher SEQUENCE, as iTIP asks.
 */
public final class CalendarInvite {

    /** iTIP method: add the event, or remove the one with the same UID. */
    public enum Method { REQUEST, CANCEL }

    private static final DateTimeFormatter UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String CRLF = "\r\n";
    private static final int MAX_OCTETS = 75;

    private CalendarInvite() {
    }

    /**
     * @param description may be null
     */
    public static String of(Method method, String uid, Instant startsAt, Instant endsAt, String summary,
                            String description, String organizerName, String organizerEmail, Instant stamp) {
        boolean cancel = method == Method.CANCEL;
        StringBuilder ics = new StringBuilder();
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Oasis Papagayo//Tennis Platform//ES");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:" + method.name());
        line(ics, "BEGIN:VEVENT");
        line(ics, "UID:" + uid);
        line(ics, "DTSTAMP:" + UTC.format(stamp));
        line(ics, "SEQUENCE:" + (cancel ? 1 : 0));
        line(ics, "STATUS:" + (cancel ? "CANCELLED" : "CONFIRMED"));
        // UTC, never a local time: no calendar can then put the lesson in the wrong zone.
        line(ics, "DTSTART:" + UTC.format(startsAt));
        line(ics, "DTEND:" + UTC.format(endsAt));
        line(ics, "SUMMARY:" + text(summary));
        if (description != null && !description.isBlank()) {
            line(ics, "DESCRIPTION:" + text(description));
        }
        line(ics, "ORGANIZER;CN=\"" + organizerName.replace("\"", "'") + "\":mailto:" + organizerEmail);
        line(ics, "END:VEVENT");
        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    /** TEXT values escape the backslash, the semicolon, the comma and the line break. */
    static String text(String value) {
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    /**
     * Appends one content line, folded so no physical line passes 75 octets: the rest goes on
     * lines that start with a space. Counted in UTF-8 octets and cut between characters, never
     * inside one, or an accent would arrive as two broken bytes.
     */
    private static void line(StringBuilder ics, String content) {
        int octets = 0;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int size = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > MAX_OCTETS) {
                ics.append(CRLF).append(' ');
                octets = 1;
            }
            ics.appendCodePoint(codePoint);
            octets += size;
            i += Character.charCount(codePoint);
        }
        ics.append(CRLF);
    }
}
