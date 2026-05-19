package com.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.List;

/// Structured event filter used by windowed event queries.
public record EventFilter(
        String text,
        String thread,
        Instant startTime,
        Instant endTime,
        List<EventFieldCondition> fieldConditions) {

    public EventFilter {
        text = text == null ? "" : text;
        thread = thread == null ? "" : thread;
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must not be before startTime");
        }
        fieldConditions = List.copyOf(fieldConditions);
    }

    public static EventFilter empty() {
        return new EventFilter("", "", null, null, List.of());
    }

    public boolean active() {
        return !text.isBlank() || !thread.isBlank() || startTime != null || endTime != null || !fieldConditions.isEmpty();
    }
}
