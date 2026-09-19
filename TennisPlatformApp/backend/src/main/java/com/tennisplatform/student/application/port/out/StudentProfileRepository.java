package com.tennisplatform.student.application.port.out;

import com.tennisplatform.student.domain.StudentProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository {

    Optional<StudentProfile> findByUserId(UUID userId);

    /** In bulk, so that rendering a listing does not cost one query per row. */
    List<StudentProfile> findAllByUserIds(Collection<UUID> userIds);

    StudentProfile save(StudentProfile profile);
}
