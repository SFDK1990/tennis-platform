package com.tennisplatform.lesson.application.port.out;

import com.tennisplatform.lesson.domain.Court;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtRepository {

    /** In their order. */
    List<Court> findAll();

    Optional<Court> findById(UUID id);

    /** Throws {@code CourtAlreadyExistsException} when another court has that name. */
    Court save(Court court);
}
