package com.tennisplatform.membership.application.port.in;

import java.util.List;
import java.util.UUID;

/** The teacher's fixed groups (decision 1). */
public interface ManageGroups {

    /** Every group of the teacher, with its members. */
    List<GroupView> ofTeacher(UUID teacherUserId);

    GroupView create(UUID teacherUserId, String name, int monthlyFeeCents);

    /** Null keeps the value. A new fee is the default for the months not yet recorded. */
    GroupView update(UUID teacherUserId, UUID groupId, String name, Integer monthlyFeeCents);

    /** Only a group nobody was ever signed up to: the fees recorded point at its memberships. */
    void delete(UUID teacherUserId, UUID groupId);

    /**
     * Adds a weekly series of the teacher to the group, and books its lessons for every member, all
     * or none, as joining does.
     */
    GroupView attachSeries(UUID teacherUserId, UUID groupId, UUID seriesId);
}
