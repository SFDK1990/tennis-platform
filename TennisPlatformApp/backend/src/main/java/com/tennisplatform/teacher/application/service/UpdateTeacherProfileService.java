package com.tennisplatform.teacher.application.service;

import com.tennisplatform.teacher.application.port.in.TeacherProfileUpdate;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import com.tennisplatform.teacher.application.port.in.UpdateTeacherProfile;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.NotTheTeacherException;
import com.tennisplatform.teacher.domain.TeacherProfile;
import com.tennisplatform.teacher.domain.TeacherProfileNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UpdateTeacherProfileService implements UpdateTeacherProfile {

    private final TeacherProfileRepository profiles;

    public UpdateTeacherProfileService(TeacherProfileRepository profiles) {
        this.profiles = profiles;
    }

    /**
     * Loads the profile <em>by the caller's own id</em> rather than loading the teacher and then
     * comparing. Both work today, but this one cannot be defeated by a future refactor that
     * forgets the comparison: asking for someone else's profile returns nothing to update.
     */
    @Override
    @Transactional
    public TeacherProfileView update(UUID callerId, TeacherProfileUpdate update) {
        TeacherProfile profile = profiles.findByUserId(callerId)
                .orElseThrow(() -> hasTeacher()
                        ? new NotTheTeacherException("This account does not own a teacher profile")
                        : new TeacherProfileNotFoundException(
                                "No teacher profile exists yet: the bootstrap has not run"));

        profile.update(update.displayName(), update.phone(), update.timezone());
        return TeacherProfileView.from(profiles.save(profile));
    }

    /** Tells apart "you are not the teacher" (403) from "there is no teacher yet" (404). */
    private boolean hasTeacher() {
        return profiles.findTheTeacher().isPresent();
    }
}
