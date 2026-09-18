package com.tennisplatform.teacher.application.service;

import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfileNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public class GetTeacherProfileService implements GetTeacherProfile {

    private final TeacherProfileRepository profiles;

    public GetTeacherProfileService(TeacherProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherProfileView get() {
        return profiles.findTheTeacher()
                .map(TeacherProfileView::from)
                .orElseThrow(() -> new TeacherProfileNotFoundException(
                        "No teacher profile exists yet: the bootstrap has not run"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeacherProfileView> byUserId(UUID userId) {
        return profiles.findByUserId(userId).map(TeacherProfileView::from);
    }
}
