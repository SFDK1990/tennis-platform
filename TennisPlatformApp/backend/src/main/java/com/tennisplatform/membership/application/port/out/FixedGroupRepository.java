package com.tennisplatform.membership.application.port.out;

import com.tennisplatform.membership.domain.FixedGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FixedGroupRepository {

    /** Translates the unique name into {@code GroupAlreadyExistsException}. */
    FixedGroup save(FixedGroup group);

    Optional<FixedGroup> findById(UUID id);

    /** In alphabetical order. */
    List<FixedGroup> findByTeacher(UUID teacherUserId);

    /** Translates a membership pointing at the group into {@code GroupInUseException}. */
    void delete(UUID id);
}
