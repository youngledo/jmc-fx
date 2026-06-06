package io.github.youngledo.jmcfx.domain.model;

import java.nio.file.Path;
import java.util.Objects;

public record HeapDumpReferencePathRequest(
        Path path,
        String selectedObjectId,
        HeapDumpReferenceDirection direction,
        int maxDepth,
        int maxPaths,
        int offset,
        int limit) {

    public HeapDumpReferencePathRequest {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(selectedObjectId, "selectedObjectId");
        Objects.requireNonNull(direction, "direction");
        if (maxDepth <= 0 || maxPaths <= 0 || limit <= 0) {
            throw new IllegalArgumentException("maxDepth, maxPaths, and limit must be positive");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }
}
