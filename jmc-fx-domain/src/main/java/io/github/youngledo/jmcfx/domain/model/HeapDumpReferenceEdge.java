package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record HeapDumpReferenceEdge(
        String sourceId,
        String targetId,
        String label,
        String referenceKind) {

    public HeapDumpReferenceEdge {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(referenceKind, "referenceKind");
    }
}
