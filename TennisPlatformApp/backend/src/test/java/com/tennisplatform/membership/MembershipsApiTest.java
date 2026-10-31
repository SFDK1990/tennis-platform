package com.tennisplatform.membership;

import com.tennisplatform.notification.RecordingMailer;
import com.tennisplatform.notification.application.port.out.Mail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Joining a fixed group and leaving it, by the student or the teacher, and every path of a seat
 * that the group pays for (43-fase28-analisis-mensualidades.md, decisions 1, 2, 4 and 5).
 */
class MembershipsApiTest extends AbstractMembershipTest {

    private static final Duration IN_TWO_DAYS = Duration.ofDays(2);

    @Autowired
    private RecordingMailer mailer;

    @Test
    @SuppressWarnings("rawtypes")
    void joiningBooksEveryLessonOfTheGroupStillToComeAndNoneThatStarted() {
        Series series = aSeriesAt(6, clock.instant().minus(Duration.ofDays(7)), clock.instant().plus(IN_TWO_DAYS),
                clock.instant().plus(IN_TWO_DAYS).plus(Duration.ofDays(7)));
        UUID group = aGroupOf(series);
        Student student = aStudentWhoMayBook();

        ResponseEntity<Map> joined = join(student, group);

        assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID membership = UUID.fromString((String) joined.getBody().get("id"));
        assertThat(lessonsWithASeatOf(student)).containsExactlyInAnyOrder(series.lessonIds().get(1),
                series.lessonIds().get(2));
        assertThat(membershipOfBooking(student, series.lessonIds().get(1))).isEqualTo(membership);
        assertThat(groupAsTheTeacher(group)).containsEntry("memberCount", 1).containsEntry("freeSeats", 5);
    }

    /** A group nobody can join in full is not joined at all: no membership and no seat. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void withOneLessonFullNobodyJoinsAndNothingIsBooked() {
        Series series = aWeeklySeries(1, IN_TWO_DAYS, 3);
        UUID group = aGroupOf(series);
        Student dropIn = aStudentWhoMayBook();
        assertThat(book(dropIn.token(), series.lessonIds().get(1)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Student student = aStudentWhoMayBook();

        ResponseEntity<Map> refused = join(student, group);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).containsEntry("code", "GROUP_FULL");
        assertThat((List<String>) refused.getBody().get("fullLessons")).hasSize(1);
        assertThat(lessonsWithASeatOf(student)).isEmpty();
        assertThat(groupAsTheTeacher(group)).containsEntry("memberCount", 0);
    }

    /** Decision 4: the seats the group does not fill are booked one by one, as any lesson. */
    @Test
    @SuppressWarnings("rawtypes")
    void theSeatsTheGroupLeavesFreeAreBookedOneByOneWithAPass() {
        Series series = aWeeklySeries(2, IN_TWO_DAYS, 2);
        UUID group = aGroupOf(series);
        aMembershipOf(aStudentWhoMayBook(), group);
        Student dropIn = aStudentWhoMayBook();

        ResponseEntity<Map> booked = book(dropIn.token(), series.lessonIds().getFirst());

        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(booked.getBody()).containsEntry("membershipId", null);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aStudentNeedsAVerifiedAddressAndTheTeachersManagementToJoin() {
        UUID group = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 1));

        ResponseEntity<Map> unverified = join(aStudentWithAProfile(), group);
        Student unmanaged = aStudentWithAProfile();
        verify(unmanaged);
        ResponseEntity<Map> notManaged = rest.exchange("/api/v1/me/groups/" + group + "/membership",
                HttpMethod.POST, new HttpEntity<>(bearer(tokenOf(unmanaged.email(), PASSWORD))), Map.class);

        assertThat(unverified.getBody()).containsEntry("code", "EMAIL_NOT_VERIFIED");
        assertThat(notManaged.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notManaged.getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");
    }

    /** The level counts as when booking: a group of another level is refused, and nothing is booked. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentOfAnotherLevelCannotJoin() {
        Series series = aWeeklySeries(4, IN_TWO_DAYS, 2);
        UUID group = aGroupOf(series);
        Student student = aStudentWhoMayBook();
        UUID beginners = aLevel("Iniciación");
        UUID advanced = aLevel("Avanzado");
        jdbc.update("UPDATE lessons SET level_id = ? WHERE series_id = ?", advanced, series.id());
        jdbc.update("UPDATE teacher_students SET level_id = ? WHERE student_user_id = ?", beginners, student.id());

        ResponseEntity<Map> refused = join(student, group);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "LEVEL_NOT_ALLOWED");
        assertThat(lessonsWithASeatOf(student)).isEmpty();
    }

    /** The fee pays for the seats: with passes required, a member needs none. */
    @Test
    @SuppressWarnings("rawtypes")
    void withPassesRequiredJoiningSpendsNoPass() {
        rest.exchange("/api/v1/teacher/pass-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("required", true), jsonBearer(teacherToken)), Map.class);
        Series series = aWeeklySeries(4, IN_TWO_DAYS, 2);
        UUID group = aGroupOf(series);
        Student student = aStudentWhoMayBook();

        assertThat(join(student, group).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(lessonsWithASeatOf(student)).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings WHERE pass_id IS NOT NULL", Integer.class))
                .isZero();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void joiningTwiceIsAConflict() {
        UUID group = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 1));
        Student student = aStudentWhoMayBook();
        aMembershipOf(student, group);

        ResponseEntity<Map> again = join(student, group);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "ALREADY_IN_GROUP");
    }

    /** Decision 2: the teacher signs a student up as the student would, without a verified address. */
    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherSignsAStudentUpAndOneEmailSaysSo() {
        Series series = aWeeklySeries(4, IN_TWO_DAYS, 3);
        UUID group = aGroupOf(series);
        Student student = aStudentWithAProfile();
        manage(student);
        mailer.clear();

        ResponseEntity<Map> added = rest.exchange("/api/v1/teacher/groups/" + group + "/members", HttpMethod.POST,
                new HttpEntity<>(Map.of("studentUserId", student.id()), jsonBearer(teacherToken)), Map.class);

        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(lessonsWithASeatOf(student)).hasSize(3);
        assertThat(mailer.to(student.email())).extracting(Mail::kind).containsExactly("group-joined-student");
        assertThat(mailer.to(TEACHER_EMAIL)).isEmpty();
    }

    @Test
    void aStudentWhoJoinsGetsOneEmailAndTheTeacherAnother() {
        UUID group = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 3));
        Student student = aStudentWhoMayBook();
        mailer.clear();

        aMembershipOf(student, group);

        assertThat(mailer.to(student.email())).extracting(Mail::kind).containsExactly("group-joined-student");
        assertThat(mailer.to(TEACHER_EMAIL)).extracting(Mail::kind).containsExactly("group-joined-teacher");
    }

    /** A new term: a series added to the group books its lessons for every member, all or none. */
    @Test
    @SuppressWarnings("rawtypes")
    void addingASeriesToTheGroupBooksItsLessonsForEveryMember() {
        UUID group = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 1));
        Student one = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        aMembershipOf(one, group);
        aMembershipOf(other, group);
        Series nextTerm = aWeeklySeries(4, IN_TWO_DAYS.plusDays(1), 2);

        assertThat(attach(group, nextTerm.id()).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(lessonsWithASeatOf(one)).hasSize(3).containsAll(nextTerm.lessonIds());
        assertThat(lessonsWithASeatOf(other)).hasSize(3).containsAll(nextTerm.lessonIds());
        ResponseEntity<Map> again = attach(aGroup("Otro", 5_000), nextTerm.id());
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "SERIES_ALREADY_IN_GROUP");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aSeriesWithNoSeatForEveryMemberIsNotAdded() {
        UUID group = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 1));
        aMembershipOf(aStudentWhoMayBook(), group);
        aMembershipOf(aStudentWhoMayBook(), group);
        Series tooSmall = aWeeklySeries(1, IN_TWO_DAYS.plusDays(1), 2);

        ResponseEntity<Map> refused = attach(group, tooSmall.id());

        assertThat(refused.getBody()).containsEntry("code", "GROUP_FULL");
        assertThat(confirmedBookingsOf(tooSmall.lessonIds().getFirst())).isZero();
        assertThat(attach(group, UUID.randomUUID()).getBody()).containsEntry("code", "SERIES_NOT_FOUND");
    }

    /**
     * A member who cannot come cancels the lesson as any booking, and the waiting list gets the seat;
     * when a seat is free again, booking it back keeps it the group's.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void aMemberCancelsOneLessonAndBooksItBackAsTheGroups() {
        Series series = aWeeklySeries(1, IN_TWO_DAYS, 1);
        UUID lesson = series.lessonIds().getFirst();
        UUID group = aGroupOf(series);
        Student member = aStudentWhoMayBook();
        UUID membership = aMembershipOf(member, group);
        Student waiting = aStudentWhoMayBook();
        rest.exchange("/api/v1/lessons/" + lesson + "/waiting-list", HttpMethod.POST,
                new HttpEntity<>(bearer(waiting.token())), Map.class);
        UUID bookingId = jdbc.queryForObject("SELECT id FROM bookings WHERE student_user_id = ?", UUID.class,
                member.id());

        rest.exchange("/api/v1/bookings/" + bookingId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(member.token())), Map.class);

        assertThat(lessonsWithASeatOf(waiting)).containsExactly(lesson);
        UUID waitingBooking = jdbc.queryForObject("SELECT id FROM bookings WHERE student_user_id = ?", UUID.class,
                waiting.id());
        rest.exchange("/api/v1/bookings/" + waitingBooking + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(waiting.token())), Map.class);
        // With passes required, only the group can pay for the seat: the member has no pass.
        rest.exchange("/api/v1/teacher/pass-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("required", true), jsonBearer(teacherToken)), Map.class);
        ResponseEntity<Map> back = book(member.token(), lesson);
        assertThat(back.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(back.getBody()).containsEntry("membershipId", membership.toString());
    }

    /**
     * Decision 5: leaving keeps the lessons of the month under way and frees the rest, which go to the
     * waiting list; the teacher hears of it.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void leavingKeepsTheMonthUnderWayAndFreesTheRest() {
        Instant nextMonth = startOf(thisMonth().plusMonths(1));
        Instant soon = clock.instant().plus(Duration.ofHours(2));
        Series series = aSeriesAt(1, soon, nextMonth.plus(Duration.ofDays(2)).plus(Duration.ofHours(10)),
                nextMonth.plus(Duration.ofDays(9)).plus(Duration.ofHours(10)));
        UUID group = aGroupOf(series);
        Student member = aStudentWhoMayBook();
        aMembershipOf(member, group);
        Student waiting = aStudentWhoMayBook();
        rest.exchange("/api/v1/lessons/" + series.lessonIds().get(1) + "/waiting-list", HttpMethod.POST,
                new HttpEntity<>(bearer(waiting.token())), Map.class);
        mailer.clear();

        ResponseEntity<Map> left = leave(member, group);

        assertThat(left.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Two hours from now may already be next month, on the last night of one.
        boolean soonIsThisMonth = soon.isBefore(nextMonth);
        assertThat(lessonsWithASeatOf(member)).containsExactlyElementsOf(
                soonIsThisMonth ? List.of(series.lessonIds().getFirst()) : List.of());
        assertThat(lessonsWithASeatOf(waiting)).containsExactly(series.lessonIds().get(1));
        assertThat(mailer.to(TEACHER_EMAIL)).extracting(Mail::kind).contains("group-left-teacher");
        assertThat(leave(member, group).getBody()).containsEntry("code", "NOT_IN_GROUP");
        assertThat(join(member, group).getBody()).containsEntry("code", "ALREADY_IN_GROUP");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherTakesAStudentOutAndTheStudentIsTold() {
        Instant nextMonth = startOf(thisMonth().plusMonths(1));
        Series series = aSeriesAt(4, nextMonth.plus(Duration.ofDays(3)));
        UUID group = aGroupOf(series);
        Student member = aStudentWhoMayBook();
        aMembershipOf(member, group);
        mailer.clear();

        ResponseEntity<Map> removed = rest.exchange("/api/v1/teacher/groups/" + group + "/members/" + member.id(),
                HttpMethod.DELETE, new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(lessonsWithASeatOf(member)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM bookings WHERE student_user_id = ?", String.class,
                member.id())).isEqualTo("CANCELLED_BY_TEACHER");
        assertThat(mailer.to(member.email())).extracting(Mail::kind).containsExactly("group-left-student");
    }

    /** Letting the student go ends the membership at once: no more months are owed. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void stoppingManagingTheStudentEndsTheirMembership() {
        Series series = aWeeklySeries(4, IN_TWO_DAYS, 2);
        UUID group = aGroupOf(series);
        Student member = aStudentWhoMayBook();
        aMembershipOf(member, group);

        rest.exchange("/api/v1/teacher/students/" + member.id() + "/manage", HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(lessonsWithASeatOf(member)).isEmpty();
        assertThat(groupAsTheTeacher(group)).containsEntry("memberCount", 0);
        assertThat(jdbc.queryForObject("SELECT left_at IS NOT NULL FROM group_memberships WHERE student_user_id = ?",
                Boolean.class, member.id())).isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theStudentSeesTheirGroupsAndAnotherStudentDoesNot() {
        UUID group = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 1));
        Student member = aStudentWhoMayBook();
        aMembershipOf(member, group);
        Student other = aStudentWhoMayBook();

        List<Map> mine = (List<Map>) rest.exchange("/api/v1/me/memberships", HttpMethod.GET,
                new HttpEntity<>(bearer(member.token())), Map.class).getBody().get("items");
        List<Map> theirs = (List<Map>) rest.exchange("/api/v1/me/memberships", HttpMethod.GET,
                new HttpEntity<>(bearer(other.token())), Map.class).getBody().get("items");
        List<Map> everybody = (List<Map>) rest.exchange("/api/v1/groups", HttpMethod.GET,
                new HttpEntity<>(bearer(other.token())), Map.class).getBody().get("items");

        assertThat(mine).hasSize(1);
        assertThat((Map<String, Object>) mine.getFirst().get("group")).containsEntry("id", group.toString());
        assertThat(theirs).isEmpty();
        assertThat(everybody).hasSize(1);
        assertThat(everybody.getFirst()).containsEntry("memberCount", 1);
        assertThat((List<?>) everybody.getFirst().get("members")).isEmpty();
        assertThat(leave(other, group).getBody()).containsEntry("code", "NOT_IN_GROUP");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aGroupSomebodyJoinedCannotBeDeletedButAnEmptyOneCan() {
        UUID used = aGroupOf(aWeeklySeries(4, IN_TWO_DAYS, 1));
        aMembershipOf(aStudentWhoMayBook(), used);
        UUID empty = aGroup("Vacío", 4_000);

        ResponseEntity<Map> refused = rest.exchange("/api/v1/teacher/groups/" + used, HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        ResponseEntity<Map> deleted = rest.exchange("/api/v1/teacher/groups/" + empty, HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(refused.getBody()).containsEntry("code", "GROUP_IN_USE");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.exchange("/api/v1/teacher/groups/" + empty, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("name", "X"), jsonBearer(teacherToken)), Map.class).getBody())
                .containsEntry("code", "GROUP_NOT_FOUND");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void twoGroupsCannotHaveTheSameName() {
        aGroup("Martes", 6_000);

        ResponseEntity<Map> again = rest.exchange("/api/v1/teacher/groups", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Martes", "monthlyFeeCents", 1), jsonBearer(teacherToken)), Map.class);
        ResponseEntity<Map> blank = rest.exchange("/api/v1/teacher/groups", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", " ", "monthlyFeeCents", 1), jsonBearer(teacherToken)), Map.class);

        assertThat(again.getBody()).containsEntry("code", "GROUP_ALREADY_EXISTS");
        assertThat(blank.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    @SuppressWarnings("rawtypes")
    private UUID aLevel(String name) {
        ResponseEntity<Map> created = rest.exchange("/api/v1/teacher/levels", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), jsonBearer(teacherToken)), Map.class);
        return UUID.fromString((String) created.getBody().get("id"));
    }
}
