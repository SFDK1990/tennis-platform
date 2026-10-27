package com.tennisplatform.student.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface StudentPhotoJpaRepository extends JpaRepository<StudentPhotoEntity, UUID> {

    /** The ids only: a list of students must not load every picture to say which have one. */
    @Query("select p.userId from StudentPhotoEntity p where p.userId in :ids")
    List<UUID> withPhoto(@Param("ids") Collection<UUID> ids);
}
