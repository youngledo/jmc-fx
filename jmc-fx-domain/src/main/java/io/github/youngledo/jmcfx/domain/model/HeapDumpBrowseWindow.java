package io.github.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record HeapDumpBrowseWindow<T>(
        List<T> rows,
        int offset,
        int limit,
        long totalCount,
        boolean truncated) {

    public HeapDumpBrowseWindow {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (totalCount < 0) {
            throw new IllegalArgumentException("totalCount must not be negative");
        }
    }
}
