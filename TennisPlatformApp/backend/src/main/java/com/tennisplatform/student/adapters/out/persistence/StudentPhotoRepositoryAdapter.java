package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.application.port.out.StudentPhotoRepository;
import com.tennisplatform.student.domain.StudentPhoto;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
class StudentPhotoRepositoryAdapter implements StudentPhotoRepository {

    private final StudentPhotoJpaRepository jpa;

    StudentPhotoRepositoryAdapter(StudentPhotoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(StudentPhoto photo, Instant at) {
        jpa.save(StudentPhotoEntity.fromDomain(photo, at));
    }

    @Override
    public Optional<StudentPhoto> findByStudent(UUID studentUserId) {
        return jpa.findById(studentUserId).map(StudentPhotoEntity::toDomain);
    }

    @Override
    public Set<UUID> studentsWithPhoto(Collection<UUID> studentUserIds) {
        return Set.copyOf(jpa.withPhoto(studentUserIds));
    }

    @Override
    public void delete(UUID studentUserId) {
        jpa.deleteById(studentUserId);
    }
}
