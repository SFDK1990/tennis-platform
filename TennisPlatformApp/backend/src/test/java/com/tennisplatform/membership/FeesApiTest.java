package com.tennisplatform.membership;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The monthly fees (43-fase28-analisis-mensualidades.md, decision 3 and "La cuota"): which months
 * are owed, how each stands, and what the teacher records. The real clock runs here, so the months
 * are taken relative to today; the due day itself is tested with a fixed clock in
 * {@code FeeCalendarTest}.
 */
class FeesApiTest extends AbstractMembershipTest {

    /** A member since two months ago, with a lesson last month and another next month. */
    private record Member(UUID membershipId, Student student) {
    }

    /**
     * The series runs from last month to next; joining books only the lesson still to come, and the
     * membership is then moved back two months, as if the student had joined then.
     */
    private Member aMemberSinceTwoMonthsAgo() {
        Instant lastMonth = startOf(thisMonth().minusMonths(1)).plus(Duration.ofDays(10)).plus(Duration.ofHours(10));
        Instant nextMonth = startOf(thisMonth().plusMonths(1)).plus(Duration.ofDays(10)).plus(Duration.ofHours(10));
        UUID group = aGroupOf(aSeriesAt(4, lastMonth, nextMonth));
        Student student = aStudentWhoMayBook();
        UUID membership = aMembershipOf(student, group);
        joinedOn(membership, startOf(thisMonth().minusMonths(2)));
        return new Member(membership, student);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aMonthIsOwedWhenTheGroupHasALessonInItAndNotOtherwise() {
        Member member = aMemberSinceTwoMonthsAgo();

        List<Map> upToNow = feeLines(null);

        assertThat(upToNow).extracting(line -> line.get("month"))
                .containsExactly(thisMonth().minusMonths(1).toString());
        assertThat(feeLines(thisMonth().minusMonths(2))).isEmpty();
        assertThat(lineOf(member.membershipId(), thisMonth().plusMonths(1))).containsEntry("status", "PENDING");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aMonthGonePastItsDueDayUnpaidIsOverdueUntilItIsPaid() {
        Member member = aMemberSinceTwoMonthsAgo();
        YearMonth lastMonth = thisMonth().minusMonths(1);
        assertThat(lineOf(member.membershipId(), lastMonth)).containsEntry("status", "OVERDUE");

        ResponseEntity<Map> paid = recordPaid(member.membershipId(), lastMonth, null);

        assertThat(paid.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(paid.getBody()).containsEntry("amountCents", 6_000).containsEntry("kind", "PAID");
        assertThat(lineOf(member.membershipId(), lastMonth)).containsEntry("status", "PAID");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aMonthRecordedTwiceIsAConflictAndOnceVoidedItCanBeRecordedAgain() {
        Member member = aMemberSinceTwoMonthsAgo();
        YearMonth lastMonth = thisMonth().minusMonths(1);
        UUID fee = UUID.fromString((String) recordPaid(member.membershipId(), lastMonth, 5_000).getBody().get("id"));

        ResponseEntity<Map> twice = recordPaid(member.membershipId(), lastMonth, 5_000);
        ResponseEntity<Map> voided = rest.exchange("/api/v1/teacher/fees/" + fee + "/void", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(twice.getBody()).containsEntry("code", "FEE_ALREADY_PAID");
        assertThat(voided.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(voided.getBody().get("voidedAt")).isNotNull();
        assertThat(lineOf(member.membershipId(), lastMonth)).containsEntry("status", "OVERDUE");
        assertThat(recordPaid(member.membershipId(), lastMonth, 5_000).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(rest.exchange("/api/v1/teacher/fees/" + UUID.randomUUID() + "/void", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody()).containsEntry("code", "FEE_NOT_FOUND");
    }

    @Test
    void aMonthThatIsNotChargedIsNeverOverdue() {
        Member member = aMemberSinceTwoMonthsAgo();
        YearMonth lastMonth = thisMonth().minusMonths(1);

        assertThat(recordWaived(member.membershipId(), lastMonth).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(lineOf(member.membershipId(), lastMonth)).containsEntry("status", "WAIVED");
    }

    /** Paying ahead is common: a month recorded in advance shows among the months up to now. */
    @Test
    @SuppressWarnings("rawtypes")
    void aMonthPaidAheadIsListedAndAMonthOutsideTheMembershipIsRefused() {
        Member member = aMemberSinceTwoMonthsAgo();

        assertThat(recordPaid(member.membershipId(), thisMonth().plusMonths(1), null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        ResponseEntity<Map> before = recordPaid(member.membershipId(), thisMonth().minusMonths(3), null);

        assertThat(feeLines(null)).extracting(line -> line.get("month"))
                .contains(thisMonth().plusMonths(1).toString());
        assertThat(before.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void changingTheFeeDoesNotRewriteWhatWasRecorded() {
        Member member = aMemberSinceTwoMonthsAgo();
        YearMonth lastMonth = thisMonth().minusMonths(1);
        recordPaid(member.membershipId(), lastMonth, null);
        UUID group = UUID.fromString((String) lineOf(member.membershipId(), lastMonth).get("groupId"));

        rest.exchange("/api/v1/teacher/groups/" + group, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("monthlyFeeCents", 7_000), jsonBearer(teacherToken)), Map.class);

        Map line = lineOf(member.membershipId(), lastMonth);
        assertThat(line).containsEntry("monthlyFeeCents", 7_000);
        assertThat(((Map<?, ?>) line.get("fee")).get("amountCents")).isEqualTo(6_000);
    }

    /** What was collected shows in the month's summary, by the day it was paid. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theMonthsSummaryAddsTheFeesCollected() {
        Member member = aMemberSinceTwoMonthsAgo();
        recordPaid(member.membershipId(), thisMonth().minusMonths(1), 4_500);
        recordWaived(member.membershipId(), thisMonth().plusMonths(1));

        Map summary = rest.exchange("/api/v1/teacher/summary", HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class).getBody();

        assertThat((Map<String, Object>) summary.get("fees")).containsEntry("feesPaid", 1).containsEntry("totalCents", 4_500);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theStudentSeesTheirMonthsAndTheDueDayCanBeChanged() {
        Member member = aMemberSinceTwoMonthsAgo();

        List<Map> mine = (List<Map>) rest.exchange("/api/v1/me/memberships", HttpMethod.GET,
                new HttpEntity<>(bearer(member.student().token())), Map.class).getBody().get("items");
        ResponseEntity<Map> changed = rest.exchange("/api/v1/teacher/fee-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("dueDay", 10), jsonBearer(teacherToken)), Map.class);
        ResponseEntity<Map> tooLate = rest.exchange("/api/v1/teacher/fee-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("dueDay", 31), jsonBearer(teacherToken)), Map.class);

        assertThat((List<Map>) mine.getFirst().get("fees")).extracting(line -> line.get("status"))
                .containsExactly("OVERDUE");
        assertThat(changed.getBody()).containsEntry("dueDay", 10);
        assertThat(lineOf(member.membershipId(), thisMonth().minusMonths(1)))
                .containsEntry("dueOn", thisMonth().minusMonths(1).atDay(10).toString());
        assertThat(tooLate.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }
}
