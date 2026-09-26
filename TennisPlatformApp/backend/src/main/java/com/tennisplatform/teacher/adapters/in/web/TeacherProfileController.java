package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.teacher.adapters.in.web.TeacherDtos.TeacherProfileResponse;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only. The teacher changes their profile through {@code PATCH /me}, like everybody else:
 * a second write path was a second place for the rules to drift apart
 * (24-fase13-analisis-revision-api.md).
 */
@RestController
@RequestMapping("/api/v1/teacher/profile")
class TeacherProfileController {

    private final GetTeacherProfile getTeacherProfile;

    TeacherProfileController(GetTeacherProfile getTeacherProfile) {
        this.getTeacherProfile = getTeacherProfile;
    }

    /**
     * Readable by any authenticated user: a student needs the teacher's name and time zone to
     * read the calendar. It carries no personal data beyond what the platform exists to show.
     */
    @GetMapping
    TeacherProfileResponse get() {
        return TeacherProfileResponse.from(getTeacherProfile.get());
    }
}
