package com.tennisplatform.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.identity.application.port.in.GetCurrentUser;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.student.application.port.in.StudentProfileUpdate;
import com.tennisplatform.student.application.port.in.StudentProfileView;
import com.tennisplatform.student.application.port.in.UpdateStudentProfile;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileUpdate;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import com.tennisplatform.teacher.application.port.in.UpdateTeacherProfile;
import com.tennisplatform.web.MeDtos.MeResponse;
import com.tennisplatform.web.MeDtos.UpdateMeRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * {@code /me}: the account plus the personal data that belongs to the caller's role.
 *
 * <p><strong>Why it lives outside every module.</strong> The response mixes fields owned by
 * three of them - the account by {@code identity}, the personal data by {@code student} or
 * {@code teacher} - and no module is allowed to own all three. Putting it in {@code identity}
 * would mean identity reading another module's tables and depending on modules it must not
 * depend on; putting it in {@code student} would make the student module serve the teacher's
 * profile. So it sits in {@code web}, which the module rules already treat as outside the
 * graph, and it composes the answer out of inbound ports only - never a domain type, never an
 * adapter. That restraint is itself a rule in {@code ModuleBoundariesTest}, so this file
 * cannot quietly become a back door into a module's internals.
 *
 * <p>The caller's id always comes from the verified token and never from a parameter, which is
 * what makes it impossible to ask for somebody else's data by editing a URL.
 */
@RestController
@RequestMapping("/api/v1/me")
class MeController {

    private final GetCurrentUser getCurrentUser;
    private final GetStudentProfile studentProfiles;
    private final UpdateStudentProfile updateStudentProfile;
    private final GetTeacherProfile teacherProfiles;
    private final UpdateTeacherProfile updateTeacherProfile;

    MeController(GetCurrentUser getCurrentUser, GetStudentProfile studentProfiles,
                 UpdateStudentProfile updateStudentProfile, GetTeacherProfile teacherProfiles,
                 UpdateTeacherProfile updateTeacherProfile) {
        this.getCurrentUser = getCurrentUser;
        this.studentProfiles = studentProfiles;
        this.updateStudentProfile = updateStudentProfile;
        this.teacherProfiles = teacherProfiles;
        this.updateTeacherProfile = updateTeacherProfile;
    }

    @GetMapping
    MeResponse me(@AuthenticationPrincipal AuthenticatedUser caller) {
        UserSummary account = getCurrentUser.byId(caller.id());

        if (caller.isTeacher()) {
            return compose(account, Optional.empty(), teacherProfiles.byUserId(caller.id()));
        }
        return compose(account, studentProfiles.byUserId(caller.id()), Optional.empty());
    }

    /**
     * Applies the fields that belong to the caller's role and refuses the rest.
     *
     * <p>A student's first call creates their profile, which is where the student profile is
     * born - registration deliberately does not create it, because the personal data is asked
     * for after the email has been verified.
     */
    @PatchMapping
    MeResponse update(@AuthenticationPrincipal AuthenticatedUser caller,
                      @Valid @RequestBody UpdateMeRequest request) {
        UserSummary account = getCurrentUser.byId(caller.id());

        if (caller.isTeacher()) {
            refuse(request.fullName(), "fullName", "TEACHER");
            refuse(request.nationalId(), "nationalId", "TEACHER");
            refuse(request.address(), "address", "TEACHER");

            TeacherProfileView updated = updateTeacherProfile.update(caller.id(),
                    new TeacherProfileUpdate(request.displayName(), request.phone(),
                            request.timezone()));
            return compose(account, Optional.empty(), Optional.of(updated));
        }

        refuse(request.displayName(), "displayName", account.role());
        refuse(request.timezone(), "timezone", account.role());

        StudentProfileView updated = updateStudentProfile.update(caller.id(),
                new StudentProfileUpdate(request.fullName(), request.phone(),
                        request.nationalId(), request.address()));
        return compose(account, Optional.of(updated), Optional.empty());
    }

    private void refuse(String value, String field, String role) {
        if (value != null) {
            throw new FieldNotApplicableToRoleException(
                    "The field '" + field + "' does not apply to a " + role + " account");
        }
    }

    private MeResponse compose(UserSummary account, Optional<StudentProfileView> student,
                               Optional<TeacherProfileView> teacher) {
        return new MeResponse(account.id(), account.email(), account.role(), account.status(),
                account.emailVerifiedAt(),
                student.map(StudentProfileView::fullName).orElse(null),
                student.map(StudentProfileView::phone)
                        .or(() -> teacher.map(TeacherProfileView::phone))
                        .orElse(null),
                student.map(StudentProfileView::nationalId).orElse(null),
                student.map(StudentProfileView::address).orElse(null),
                teacher.map(TeacherProfileView::displayName).orElse(null),
                teacher.map(TeacherProfileView::timezone).orElse(null));
    }
}
