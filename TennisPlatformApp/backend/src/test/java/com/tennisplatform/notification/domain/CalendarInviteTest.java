package com.tennisplatform.notification.domain;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarInviteTest {

    private static final Instant STARTS = Instant.parse("2026-10-14T08:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-10-14T09:00:00Z");
    private static final Instant STAMP = Instant.parse("2026-10-12T18:30:05Z");

    private static String invite(CalendarInvite.Method method, String summary, String description) {
        return CalendarInvite.of(method, "b-1@tennis-platform", STARTS, ENDS, summary, description,
                "Escuela de Tenis Oasis Papagayo", "no-reply@example.com", STAMP);
    }

    @Test
    void theCancellationNamesTheSameEventAsTheInvitationSoTheCalendarRemovesIt() {
        String request = invite(CalendarInvite.Method.REQUEST, "Clase", null);
        String cancel = invite(CalendarInvite.Method.CANCEL, "Clase", null);

        assertThat(request).contains("\r\nMETHOD:REQUEST\r\n", "\r\nUID:b-1@tennis-platform\r\n",
                "\r\nSEQUENCE:0\r\n", "\r\nSTATUS:CONFIRMED\r\n");
        assertThat(cancel).contains("\r\nMETHOD:CANCEL\r\n", "\r\nUID:b-1@tennis-platform\r\n",
                "\r\nSEQUENCE:1\r\n", "\r\nSTATUS:CANCELLED\r\n");
    }

    @Test
    void timesAreWrittenInUtcSoNoCalendarPutsTheLessonInAnotherZone() {
        assertThat(invite(CalendarInvite.Method.REQUEST, "Clase", null))
                .contains("\r\nDTSTART:20261014T080000Z\r\n", "\r\nDTEND:20261014T090000Z\r\n",
                        "\r\nDTSTAMP:20261012T183005Z\r\n");
    }

    @Test
    void commasSemicolonsBackslashesAndLineBreaksAreEscaped() {
        String ics = invite(CalendarInvite.Method.CANCEL, "Clase", "Lluvia, viento; y\nfrio \\ todo");

        assertThat(ics).contains("DESCRIPTION:Lluvia\\, viento\\; y\\nfrio \\\\ todo\r\n");
    }

    @Test
    void everyLineEndsInCrlfAndNoneIsLongerThan75Octets() {
        String longSummary = "Clase de tenis con Ana Serrano en la pista central, la del fondo, junto al bar ".repeat(3)
                + "y con más acentos: ñ á é í ó ú";
        String ics = invite(CalendarInvite.Method.REQUEST, longSummary, null);

        assertThat(ics).endsWith("END:VCALENDAR\r\n");
        assertThat(ics.replace("\r\n", "")).doesNotContain("\r", "\n");
        assertThat(Arrays.stream(ics.split("\r\n")))
                .allSatisfy(line -> assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75));
    }

    @Test
    void aFoldedLineUnfoldsToWhatWasWrittenWithoutBreakingAnAccent() {
        String summary = "Clase con acentos ".repeat(4) + "ñandú camión";
        String ics = invite(CalendarInvite.Method.REQUEST, summary, null);

        // Unfolding is removing every CRLF followed by a space (RFC 5545, 3.1).
        assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + summary + "\r\n");
    }
}
