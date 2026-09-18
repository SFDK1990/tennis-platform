package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Wire representations. Kept apart from the domain so persistence can never leak out. */
final class TeacherDtos {

    private TeacherDtos() {
    }

    record TeacherProfileResponse(UUID userId, String displayName, String phone, String timezone) {

        static TeacherProfileResponse from(TeacherProfileView view) {
            return new TeacherProfileResponse(view.userId(), view.displayName(), view.phone(),
                    view.timezone());
        }
    }

    /**
     * Every field is optional: this is a PATCH, and null means "not submitted". The lengths are
     * checked here only to reject obvious junk early - the rules that matter live in the domain,
     * which is what guarantees they hold no matter who calls.
     */
    record UpdateTeacherProfileRequest(
            @Size(max = 255) String displayName,
            @Size(max = 30) String phone,
            @Size(max = 60) String timezone) {
    }
}
