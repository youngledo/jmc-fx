package io.github.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record HeapDumpReferencePath(
        String selectedObjectId,
        List<HeapDumpReferenceEdge> edges,
        long retainedSizeBytes,
        boolean truncated) {

    public HeapDumpReferencePath {
        Objects.requireNonNull(selectedObjectId, "selectedObjectId");
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
        if (retainedSizeBytes < 0) {
            throw new IllegalArgumentException("retainedSizeBytes must not be negative");
        }
    }
}
