package com.tennisplatform.pass.application.port.in;

import com.tennisplatform.pass.domain.PassType;

import java.util.UUID;

/** A pass type as anybody outside this module sees it; the lesson type travels as its wire value. */
public record PassTypeView(UUID id, String name, String lessonType, int lessons, int priceCents,
                           Integer validityMonths, boolean retired) {

    public static PassTypeView from(PassType type) {
        return new PassTypeView(type.id(), type.name(), type.lessonType().name(), type.lessons(), type.priceCents(),
                type.validityMonths(), type.retired());
    }
}
