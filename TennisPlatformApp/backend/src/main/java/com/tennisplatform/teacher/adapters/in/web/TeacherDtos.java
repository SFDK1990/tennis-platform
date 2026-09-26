package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.teacher.application.port.in.TeacherProfileView;

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
}
