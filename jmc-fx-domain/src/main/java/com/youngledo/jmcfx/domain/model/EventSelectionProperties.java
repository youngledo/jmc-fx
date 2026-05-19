package com.youngledo.jmcfx.domain.model;

import java.util.List;

/// Properties computed for the currently selected Event Browser item collection.
public record EventSelectionProperties(
        String selectionId,
        String label,
        long eventCount,
        List<EventProperty> properties) {

    public EventSelectionProperties {
        if (selectionId == null || selectionId.isBlank()) {
            throw new IllegalArgumentException("selectionId must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (eventCount < 0) {
            throw new IllegalArgumentException("eventCount must be >= 0");
        }
        properties = List.copyOf(properties);
    }
}
