package com.tennisplatform.student.application.port.out;

import com.tennisplatform.student.domain.StudentPhoto;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StudentPhotoRepository {

    /** Inserts the photo, or replaces the one the student had. */
    void save(StudentPhoto photo, Instant at);

    Optional<StudentPhoto> findByStudent(UUID studentUserId);

    Set<UUID> studentsWithPhoto(Collection<UUID> studentUserIds);

    void delete(UUID studentUserId);
}
