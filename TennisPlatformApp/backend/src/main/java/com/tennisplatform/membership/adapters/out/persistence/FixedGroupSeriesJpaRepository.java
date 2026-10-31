package com.tennisplatform.membership.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface FixedGroupSeriesJpaRepository extends JpaRepository<FixedGroupSeriesEntity, UUID> {

    /** An INSERT and not a save: a save with the id set would move the series to the other group. */
    @Modifying
    @Query(value = "INSERT INTO fixed_group_series (series_id, group_id) VALUES (:seriesId, :groupId)",
            nativeQuery = true)
    void insert(@Param("groupId") UUID groupId, @Param("seriesId") UUID seriesId);

    List<FixedGroupSeriesEntity> findAllByGroupIdIn(Collection<UUID> groupIds);
}
