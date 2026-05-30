package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record JfrMetadataField(
        String id,
        String label,
        String description,
        EventValueType valueType,
        String unit) {

    public JfrMetadataField {
        id = Objects.requireNonNullElse(id, "");
        label = Objects.requireNonNullElse(label, "");
        description = Objects.requireNonNullElse(description, "");
        valueType = valueType == null ? EventValueType.TEXT : valueType;
        unit = Objects.requireNonNullElse(unit, "");
    }
}
