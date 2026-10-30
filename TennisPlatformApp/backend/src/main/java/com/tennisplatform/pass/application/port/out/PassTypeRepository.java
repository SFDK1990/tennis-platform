package com.tennisplatform.pass.application.port.out;

import com.tennisplatform.pass.domain.PassType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassTypeRepository {

    List<PassType> findByTeacher(UUID teacherUserId);

    Optional<PassType> findById(UUID id);

    /** Raises {@code PassTypeAlreadyExistsException} for a name the teacher already uses. */
    PassType save(PassType type);

    /** Raises {@code PassTypeInUseException} while a pass of the type exists. */
    void delete(UUID id);
}
