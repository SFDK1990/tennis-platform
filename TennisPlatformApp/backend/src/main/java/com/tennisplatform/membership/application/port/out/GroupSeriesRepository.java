package com.tennisplatform.membership.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Which weekly series make up each group. A series is in one group at most. */
public interface GroupSeriesRepository {

    /** Translates a series already in a group into {@code SeriesAlreadyInGroupException}. */
    void attach(UUID groupId, UUID seriesId);

    List<UUID> seriesOf(UUID groupId);

    /** The series of each group; groups with none are absent from the map. */
    Map<UUID, List<UUID>> seriesOf(Collection<UUID> groupIds);

    Optional<UUID> groupOf(UUID seriesId);
}
