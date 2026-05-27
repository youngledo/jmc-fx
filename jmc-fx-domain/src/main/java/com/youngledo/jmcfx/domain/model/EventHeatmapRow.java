package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record EventHeatmapRow(
        String eventTypeId,
        String label,
        List<String> categoryPath,
        long totalCount,
        List<EventHeatmapCell> cells) {

    public EventHeatmapRow {
        eventTypeId = Objects.requireNonNullElse(eventTypeId, "");
        label = Objects.requireNonNullElse(label, "");
        if (label.isBlank()) {
            label = eventTypeId;
        }
        categoryPath = List.copyOf(Objects.requireNonNullElse(categoryPath, List.of()));
        cells = List.copyOf(Objects.requireNonNullElse(cells, List.of()));
        if (totalCount < 0) {
            throw new IllegalArgumentException("totalCount must be >= 0");
        }
    }
}
