package com.tennisplatform.membership;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What every membership test needs: weekly series written straight through the repository, as the
 * booking tests write their lessons, groups made through the API, and students in them.
 */
abstract class AbstractMembershipTest extends AbstractBookingTest {

    protected static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Autowired
    private LessonRepository lessons;

    /** A series of group lessons, one at each instant, sharing a series id. */
    protected Series aSeriesAt(int capacity, Instant... starts) {
        UUID seriesId = UUID.randomUUID();
        List<UUID> ids = new ArrayList<>();
        for (Instant startsAt : starts) {
            Instant at = startsAt.truncatedTo(ChronoUnit.MINUTES);
            ids.add(lessons.save(Lesson.rehydrate(null, teacherId, LessonType.GROUP, at, at.plus(Duration.ofHours(1)),
                    capacity, null, false, null, seriesId)).id());
        }
        return new Series(seriesId, ids);
    }

    /** A series whose lessons start {@code fromNow} after now, one a week. */
    protected Series aWeeklySeries(int capacity, Duration fromNow, int weeks) {
        Instant[] starts = new Instant[weeks];
        for (int week = 0; week < weeks; week++) {
            starts[week] = clock.instant().plus(fromNow).plus(Duration.ofDays(7L * week));
        }
        return aSeriesAt(capacity, starts);
    }

    protected record Series(UUID id, List<UUID> lessonIds) {
    }

    @SuppressWarnings("rawtypes")
    protected UUID aGroup(String name, int monthlyFeeCents) {
        ResponseEntity<Map> created = rest.exchange("/api/v1/teacher/groups", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name, "monthlyFeeCents", monthlyFeeCents), jsonBearer(teacherToken)),
                Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) created.getBody().get("id"));
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> attach(UUID groupId, UUID seriesId) {
        return rest.exchange("/api/v1/teacher/groups/" + groupId + "/series", HttpMethod.POST,
                new HttpEntity<>(Map.of("seriesId", seriesId), jsonBearer(teacherToken)), Map.class);
    }

    /** A group of 60 € a month made of {@code series}. */
    @SuppressWarnings("rawtypes")
    protected UUID aGroupOf(Series series) {
        UUID group = aGroup("Grupo " + UUID.randomUUID().toString().substring(0, 8), 6_000);
        assertThat(attach(group, series.id()).getStatusCode()).isEqualTo(HttpStatus.OK);
        return group;
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> join(Student student, UUID groupId) {
        return rest.exchange("/api/v1/me/groups/" + groupId + "/membership", HttpMethod.POST,
                new HttpEntity<>(bearer(student.token())), Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected UUID aMembershipOf(Student student, UUID groupId) {
        ResponseEntity<Map> joined = join(student, groupId);
        assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) joined.getBody().get("id"));
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> leave(Student student, UUID groupId) {
        return rest.exchange("/api/v1/me/groups/" + groupId + "/membership", HttpMethod.DELETE,
                new HttpEntity<>(bearer(student.token())), Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> recordPaid(UUID membershipId, YearMonth month, Integer amountCents) {
        Map<String, Object> body = new HashMap<>(Map.of("membershipId", membershipId, "month", month.toString(),
                "kind", "PAID", "paidOn", today().toString(), "paymentMethod", "BIZUM"));
        body.put("amountCents", amountCents);
        return rest.exchange("/api/v1/teacher/fees", HttpMethod.POST, new HttpEntity<>(body, jsonBearer(teacherToken)),
                Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> recordWaived(UUID membershipId, YearMonth month) {
        return rest.exchange("/api/v1/teacher/fees", HttpMethod.POST, new HttpEntity<>(Map.of("membershipId",
                membershipId, "month", month.toString(), "kind", "WAIVED"), jsonBearer(teacherToken)), Map.class);
    }

    /** The lines of the teacher's fees, of one month or, with null, up to now. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected List<Map> feeLines(YearMonth month) {
        String query = month == null ? "" : "?month=" + month;
        ResponseEntity<Map> found = rest.exchange("/api/v1/teacher/fees" + query, HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map>) found.getBody().get("items");
    }

    @SuppressWarnings("rawtypes")
    protected Map lineOf(UUID membershipId, YearMonth month) {
        return feeLines(month).stream()
                .filter(line -> membershipId.toString().equals(line.get("membershipId")))
                .findFirst()
                .orElseThrow();
    }

    /** The teacher's view of the group. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Map groupAsTheTeacher(UUID groupId) {
        List<Map> groups = (List<Map>) rest.exchange("/api/v1/teacher/groups", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("items");
        return groups.stream().filter(group -> groupId.toString().equals(group.get("id"))).findFirst().orElseThrow();
    }

    protected List<UUID> lessonsWithASeatOf(Student student) {
        return jdbc.queryForList("SELECT lesson_id FROM bookings WHERE student_user_id = ? AND status = 'CONFIRMED'",
                UUID.class, student.id());
    }

    protected UUID membershipOfBooking(Student student, UUID lessonId) {
        return jdbc.queryForObject("SELECT membership_id FROM bookings WHERE student_user_id = ? AND lesson_id = ?"
                + " AND status = 'CONFIRMED'", UUID.class, student.id(), lessonId);
    }

    /** Moves the membership's start back, as if the student had joined then. */
    protected void joinedOn(UUID membershipId, Instant joinedAt) {
        jdbc.update("UPDATE group_memberships SET joined_at = ? WHERE id = ?",
                java.sql.Timestamp.from(joinedAt), membershipId);
    }

    protected LocalDate today() {
        return LocalDate.now(clock.withZone(MADRID));
    }

    protected YearMonth thisMonth() {
        return YearMonth.from(today());
    }

    /** Midnight at the start of {@code month} in Madrid. */
    protected static Instant startOf(YearMonth month) {
        return month.atDay(1).atStartOfDay(MADRID).toInstant();
    }
}
