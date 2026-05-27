package com.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record EventHeatmap(
        Instant recordingStart,
        Instant recordingEnd,
        int bucketCount,
        List<EventHeatmapRow> rows) {

    public EventHeatmap {
        recordingStart = Objects.requireNonNull(recordingStart, "recordingStart");
        recordingEnd = Objects.requireNonNull(recordingEnd, "recordingEnd");
        rows = List.copyOf(Objects.requireNonNullElse(rows, List.of()));
        if (bucketCount < 1) {
            throw new IllegalArgumentException("bucketCount must be >= 1");
        }
    }

    public long maxCellCount() {
        return rows.stream()
                .flatMap(row -> row.cells().stream())
                .mapToLong(EventHeatmapCell::count)
                .max()
                .orElse(0);
    }
}
