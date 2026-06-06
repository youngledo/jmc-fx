package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record HeapDumpObjectGroup(
        String id,
        String label,
        HeapDumpObjectGroupKind kind,
        long objectCount,
        long shallowSizeBytes,
        long retainedSizeBytes,
        long wasteBytes,
        boolean retainedSizeAvailable) {

    public HeapDumpObjectGroup {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(kind, "kind");
        if (objectCount < 0 || shallowSizeBytes < 0 || retainedSizeBytes < 0 || wasteBytes < 0) {
            throw new IllegalArgumentException("heap dump sizes and counts must not be negative");
        }
    }
}
