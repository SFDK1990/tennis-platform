package com.tennisplatform.pass.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * The teacher's pass types and whether booking needs a pass (decisions 2 and 6). Anybody signed
 * in reads them; who may change them is decided at the web edge.
 */
public interface ManagePassTypes {

    /** The teacher's types, retired ones included, by name. */
    List<PassTypeView> ofTheTeacher();

    PassTypeView create(UUID teacherUserId, NewPassType type);

    PassTypeView update(UUID teacherUserId, UUID passTypeId, PassTypeChange change);

    /** Only a type nobody has bought: one that was sold can be retired instead. */
    void delete(UUID teacherUserId, UUID passTypeId);

    /** Whether the teacher has switched on "no booking without a pass". Off until they do. */
    boolean passesRequired();

    boolean requirePasses(UUID teacherUserId, boolean required);

    record NewPassType(String name, String lessonType, int lessons, int priceCents, Integer validityMonths) {
    }

    /**
     * Every field optional. {@code validityMonths} null keeps the current validity, and
     * {@code neverExpires} true removes it.
     */
    record PassTypeChange(String name, String lessonType, Integer lessons, Integer priceCents,
                          Integer validityMonths, boolean neverExpires, Boolean retired) {
    }
}
