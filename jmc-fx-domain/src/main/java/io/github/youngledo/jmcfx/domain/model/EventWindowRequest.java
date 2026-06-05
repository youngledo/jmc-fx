package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

/// Request for the visible and prefetched event row window.
public record EventWindowRequest(
        EventTypeSelection selection,
        int visibleStartRow,
        int visibleRowCount,
        int prefetchBefore,
        int prefetchAfter,
        List<String> columnFieldIds,
        EventFilter filter) {

    public EventWindowRequest(String eventTypeId, int visibleStartRow, int visibleRowCount, int prefetchBefore,
            int prefetchAfter, List<String> columnFieldIds, EventFilter filter) {
        this(EventTypeSelection.single(eventTypeId, eventTypeId), visibleStartRow, visibleRowCount, prefetchBefore,
                prefetchAfter, columnFieldIds, filter);
    }

    public EventWindowRequest {
        if (selection == null) {
            throw new IllegalArgumentException("selection must not be null");
        }
        if (visibleStartRow < 0) {
            throw new IllegalArgumentException("visibleStartRow must be >= 0");
        }
        if (visibleRowCount <= 0) {
            throw new IllegalArgumentException("visibleRowCount must be > 0");
        }
        if (prefetchBefore < 0 || prefetchAfter < 0) {
            throw new IllegalArgumentException("prefetch values must be >= 0");
        }
        columnFieldIds = List.copyOf(columnFieldIds);
        filter = filter == null ? EventFilter.empty() : filter;
    }

    public String eventTypeId() {
        return selection.id();
    }

    public int loadStartRow() {
        return Math.max(0, visibleStartRow - prefetchBefore);
    }

    public int loadRowCount() {
        int rowsBefore = visibleStartRow - loadStartRow();
        return rowsBefore + visibleRowCount + prefetchAfter;
    }
}
