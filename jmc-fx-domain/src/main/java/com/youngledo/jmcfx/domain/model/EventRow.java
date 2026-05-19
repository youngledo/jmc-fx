package com.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Map;

/// Windowed table row with only the values needed for visible columns.
public record EventRow(
        String id,
        String eventTypeId,
        Instant startTime,
        String startTimeText,
        long durationNanos,
        String durationText,
        String threadName,
        Map<String, String> fieldValues) {

    public EventRow {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (eventTypeId == null || eventTypeId.isBlank()) {
            throw new IllegalArgumentException("eventTypeId must not be blank");
        }
        fieldValues = Map.copyOf(fieldValues);
    }
}
