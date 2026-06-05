package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record EventHeatmapCell(
        String eventTypeId,
        Instant bucketStart,
        Instant bucketEnd,
        long count) {

    public EventHeatmapCell {
        eventTypeId = Objects.requireNonNullElse(eventTypeId, "");
        bucketStart = Objects.requireNonNull(bucketStart, "bucketStart");
        bucketEnd = Objects.requireNonNull(bucketEnd, "bucketEnd");
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
    }
}
