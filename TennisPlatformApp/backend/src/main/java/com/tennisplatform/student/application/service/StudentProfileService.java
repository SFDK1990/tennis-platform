package com.tennisplatform.student.application.service;

import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.student.application.port.in.StudentProfileUpdate;
import com.tennisplatform.student.application.port.in.StudentProfileView;
import com.tennisplatform.student.application.port.in.UpdateStudentProfile;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.domain.InvalidStudentProfileException;
import com.tennisplatform.student.domain.StudentProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Reading and writing a student's own personal data.
 *
 * <p>Both use cases live in one service because they are two halves of the same thing and share
 * the rule that matters: the row is addressed by the caller's own id, never by one that arrived
 * in a request.
 */
public class StudentProfileService implements GetStudentProfile, UpdateStudentProfile {

    private final StudentProfileRepository profiles;
    private final Clock clock;

    public StudentProfileService(StudentProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentProfileView> byUserId(UUID userId) {
        return profiles.findByUserId(userId).map(StudentProfileView::from);
    }

    /**
     * Creates the profile on the first call and patches it afterwards. The creation is not a
     * separate use case because the client cannot tell the two apart: from the student's point
     * of view there is one action, "save my data", and asking them to know whether a row exists
     * would be asking them about our schema.
     */
    @Override
    @Transactional
    public StudentProfileView update(UUID callerId, StudentProfileUpdate update) {
        StudentProfile profile = profiles.findByUserId(callerId)
                .map(existing -> patch(existing, update))
                .orElseGet(() -> create(callerId, update));

        return StudentProfileView.from(profiles.save(profile));
    }

    private StudentProfile patch(StudentProfile profile, StudentProfileUpdate update) {
        profile.update(update.fullName(), update.phone(), update.nationalId(), update.address());
        return profile;
    }

    /**
     * The first save has to carry the name. The domain would refuse a blank one anyway; saying
     * it here lets the answer name the missing field instead of describing a column.
     */
    private StudentProfile create(UUID userId, StudentProfileUpdate update) {
        if (update.fullName() == null || update.fullName().isBlank()) {
            throw new InvalidStudentProfileException(
                    "The full name is required the first time the profile is filled in");
        }
        return StudentProfile.create(userId, update.fullName(), update.phone(),
                update.nationalId(), update.address(), clock.instant());
    }
}
