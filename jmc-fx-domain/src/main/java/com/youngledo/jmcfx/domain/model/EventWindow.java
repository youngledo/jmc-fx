package com.youngledo.jmcfx.domain.model;

import java.util.List;

/// Result for a loaded event row window.
public record EventWindow(
        String eventTypeId,
        int startRow,
        List<EventRow> rows,
        long totalCount,
        boolean exactTotalCount,
        EventLoadState state) {

    public EventWindow {
        if (eventTypeId == null || eventTypeId.isBlank()) {
            throw new IllegalArgumentException("eventTypeId must not be blank");
        }
        if (startRow < 0) {
            throw new IllegalArgumentException("startRow must be >= 0");
        }
        if (totalCount < -1) {
            throw new IllegalArgumentException("totalCount must be >= -1");
        }
        rows = List.copyOf(rows);
    }
}
