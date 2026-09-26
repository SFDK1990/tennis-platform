package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.teacher.adapters.in.web.TeacherDtos.TeacherProfileResponse;
import com.tennisplatform.teacher.adapters.in.web.TeacherDtos.UpdateTeacherProfileRequest;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileUpdate;
import com.tennisplatform.teacher.application.port.in.UpdateTeacherProfile;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/profile")
class TeacherProfileController {

    private final GetTeacherProfile getTeacherProfile;
    private final UpdateTeacherProfile updateTeacherProfile;

    TeacherProfileController(GetTeacherProfile getTeacherProfile,
                             UpdateTeacherProfile updateTeacherProfile) {
        this.getTeacherProfile = getTeacherProfile;
        this.updateTeacherProfile = updateTeacherProfile;
    }

    /**
     * Readable by any authenticated user: a student needs the teacher's name and time zone to
     * read the calendar. It carries no personal data beyond what the platform exists to show.
     */
    @GetMapping
    TeacherProfileResponse get() {
        return TeacherProfileResponse.from(getTeacherProfile.get());
    }

    /**
     * The role is checked here and the ownership inside the service. Neither check is enough on
     * its own: the role alone would let any future second teacher edit this one's profile, and
     * ownership alone would let a student who somehow owned a profile row edit it.
     */
    @PatchMapping
    TeacherProfileResponse update(@AuthenticationPrincipal AuthenticatedUser caller,
                                  @Valid @RequestBody UpdateTeacherProfileRequest request) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can change the teacher profile");
        }
        return TeacherProfileResponse.from(updateTeacherProfile.update(caller.id(),
                new TeacherProfileUpdate(request.displayName(), request.phone(),
                        request.timezone())));
    }
}
