package com.tennisplatform.student.application.service;

import com.tennisplatform.student.application.port.in.StudentPhotos;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentPhotoRepository;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.StudentPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** The log says whose photo changed, never anything about the picture. */
public class StudentPhotoService implements StudentPhotos {

    private static final Logger log = LoggerFactory.getLogger(StudentPhotoService.class);

    private final StudentPhotoRepository photos;
    private final ManagedStudentRepository relationships;
    private final Clock clock;

    public StudentPhotoService(StudentPhotoRepository photos, ManagedStudentRepository relationships, Clock clock) {
        this.photos = photos;
        this.relationships = relationships;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void change(UUID studentUserId, byte[] image) {
        photos.save(StudentPhoto.of(studentUserId, image), clock.instant());
        log.info("Photo of student {} changed", studentUserId);
    }

    @Override
    @Transactional
    public void remove(UUID studentUserId) {
        photos.delete(studentUserId);
        log.info("Photo of student {} removed", studentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PhotoView> ofStudent(UUID studentUserId) {
        return photos.findByStudent(studentUserId).map(StudentPhotoService::view);
    }

    /** By relationship, not by role: a teacher who let the student go no longer sees them. */
    @Override
    @Transactional(readOnly = true)
    public Optional<PhotoView> ofManagedStudent(UUID teacherUserId, UUID studentUserId) {
        boolean managed = relationships.findByPair(teacherUserId, studentUserId)
                .map(ManagedStudent::isManaged)
                .orElse(false);
        return managed ? ofStudent(studentUserId) : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> withPhoto(Collection<UUID> studentUserIds) {
        return studentUserIds.isEmpty() ? Set.of() : photos.studentsWithPhoto(studentUserIds);
    }

    private static PhotoView view(StudentPhoto photo) {
        return new PhotoView(photo.contentType(), photo.bytes());
    }
}
