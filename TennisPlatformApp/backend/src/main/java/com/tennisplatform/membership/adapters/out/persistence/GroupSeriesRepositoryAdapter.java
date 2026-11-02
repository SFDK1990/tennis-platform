package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.domain.SeriesAlreadyInGroupException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class GroupSeriesRepositoryAdapter implements GroupSeriesRepository {

    private final FixedGroupSeriesJpaRepository jpa;

    GroupSeriesRepositoryAdapter(FixedGroupSeriesJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void attach(UUID groupId, UUID seriesId) {
        try {
            jpa.insert(groupId, seriesId);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.is(e, ConstraintViolations.UNIQUE_VIOLATION)) {
                throw new SeriesAlreadyInGroupException("This series already belongs to a group");
            }
            throw e;
        }
    }

    @Override
    public List<UUID> seriesOf(UUID groupId) {
        return seriesOf(List.of(groupId)).getOrDefault(groupId, List.of());
    }

    @Override
    public Map<UUID, List<UUID>> seriesOf(Collection<UUID> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return jpa.findAllByGroupIdIn(groupIds).stream()
                .collect(Collectors.groupingBy(FixedGroupSeriesEntity::groupId,
                        Collectors.mapping(FixedGroupSeriesEntity::seriesId, Collectors.toList())));
    }

    @Override
    public Optional<UUID> groupOf(UUID seriesId) {
        return jpa.findById(seriesId).map(FixedGroupSeriesEntity::groupId);
    }
}
