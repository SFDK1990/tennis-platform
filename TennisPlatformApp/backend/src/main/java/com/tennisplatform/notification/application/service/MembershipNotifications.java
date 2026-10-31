package com.tennisplatform.notification.application.service;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.membership.application.port.in.GroupView;
import com.tennisplatform.membership.application.port.in.ManageGroups;
import com.tennisplatform.membership.application.port.in.MembershipEnding;
import com.tennisplatform.membership.application.port.in.MembershipStarted;
import com.tennisplatform.notification.application.port.in.NotifyMemberships;
import com.tennisplatform.notification.application.port.out.Mailer;
import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.student.application.port.in.StudentProfileView;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Who hears about a group joined or left, and what they are told. Whoever made the change is not
 * told about it: the student who joins gets their email, and the teacher hears of it; the teacher
 * who signs a student up already knows.
 */
public class MembershipNotifications implements NotifyMemberships {

    private static final String DELETED = "DELETED";

    private final Mailer mailer;
    private final MailTexts texts;
    private final FindUserAccounts accounts;
    private final GetStudentProfile students;
    private final GetTeacherProfile teachers;
    private final ManageGroups groups;

    public MembershipNotifications(Mailer mailer, MailTexts texts, FindUserAccounts accounts,
                                   GetStudentProfile students, GetTeacherProfile teachers, ManageGroups groups) {
        this.mailer = mailer;
        this.texts = texts;
        this.accounts = accounts;
        this.students = students;
        this.teachers = teachers;
        this.groups = groups;
    }

    @Override
    public void started(MembershipStarted membership) {
        TeacherProfileView teacher = teachers.byUserId(membership.teacherUserId()).orElseThrow();
        MailTexts.Group group = group(membership.teacherUserId(), membership.groupId(), teacher);
        Map<UUID, UserSummary> found = accounts.byIds(List.of(membership.teacherUserId(), membership.studentUserId()));
        UserSummary student = student(found.get(membership.studentUserId()));
        String name = students.byUserId(membership.studentUserId()).map(StudentProfileView::fullName).orElse(null);

        if (student != null) {
            mailer.send(texts.joinedGroupToStudent(membership.membershipId(), group, student.email(), name,
                    membership.byStudent()));
        }
        UserSummary teacherAccount = found.get(membership.teacherUserId());
        if (membership.byStudent() && teacherAccount != null) {
            mailer.send(texts.joinedGroupToTeacher(membership.membershipId(), group, teacherAccount.email(),
                    label(name, student)));
        }
    }

    @Override
    public void ending(MembershipEnding membership) {
        TeacherProfileView teacher = teachers.byUserId(membership.teacherUserId()).orElseThrow();
        MailTexts.Group group = group(membership.teacherUserId(), membership.groupId(), teacher);
        Map<UUID, UserSummary> found = accounts.byIds(List.of(membership.teacherUserId(), membership.studentUserId()));
        UserSummary student = student(found.get(membership.studentUserId()));
        String name = students.byUserId(membership.studentUserId()).map(StudentProfileView::fullName).orElse(null);

        if (membership.byStudent()) {
            UserSummary teacherAccount = found.get(membership.teacherUserId());
            if (teacherAccount != null) {
                mailer.send(texts.leftGroupToTeacher(membership.membershipId(), group, membership.leftAt(),
                        teacherAccount.email(), label(name, student)));
            }
        } else if (student != null) {
            mailer.send(texts.leftGroupToStudent(membership.membershipId(), group, membership.leftAt(),
                    student.email(), name));
        }
    }

    private MailTexts.Group group(UUID teacherUserId, UUID groupId, TeacherProfileView teacher) {
        ZoneId zone = ZoneId.of(teacher.timezone());
        GroupView view = groups.ofTeacher(teacherUserId).stream()
                .filter(found -> found.id().equals(groupId))
                .findFirst()
                .orElseThrow();
        GroupView.SeriesView next = view.series().stream()
                .filter(series -> series.nextStartsAt() != null)
                .findFirst()
                .orElse(null);
        MailTexts.Lesson first = next == null ? null
                : new MailTexts.Lesson(null, null, next.lessonType(), next.nextStartsAt(), next.nextEndsAt(), zone,
                        teacher.displayName());
        int upcoming = view.series().stream().mapToInt(GroupView.SeriesView::upcomingLessons).sum();
        return new MailTexts.Group(view.name(), view.monthlyFeeCents(), upcoming, first, zone, teacher.displayName());
    }

    /** A deleted account keeps a made-up address that nobody reads. */
    private static UserSummary student(UserSummary account) {
        return account != null && !DELETED.equals(account.status()) ? account : null;
    }

    private static String label(String name, UserSummary student) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return student != null ? Objects.requireNonNull(student.email()) : "Un alumno";
    }
}
