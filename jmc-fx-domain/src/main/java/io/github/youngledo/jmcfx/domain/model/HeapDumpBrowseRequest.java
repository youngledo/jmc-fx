package io.github.youngledo.jmcfx.domain.model;

import java.nio.file.Path;
import java.util.Objects;

public record HeapDumpBrowseRequest(
        Path path,
        HeapDumpObjectGroupKind groupKind,
        HeapDumpBrowseSort sort,
        boolean ascending,
        int offset,
        int limit,
        String searchText) {

    public HeapDumpBrowseRequest {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(groupKind, "groupKind");
        Objects.requireNonNull(sort, "sort");
        searchText = searchText == null ? "" : searchText.trim();
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
