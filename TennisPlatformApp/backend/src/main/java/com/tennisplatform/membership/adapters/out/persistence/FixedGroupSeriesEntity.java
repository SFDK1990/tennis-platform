package com.tennisplatform.membership.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Read only: a series is attached with an INSERT, so a second group for it fails instead of moving it. */
@Entity
@Table(name = "fixed_group_series")
class FixedGroupSeriesEntity {

    @Id
    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    protected FixedGroupSeriesEntity() {
    }

    UUID seriesId() {
        return seriesId;
    }

    UUID groupId() {
        return groupId;
    }
}
