package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record HeapDumpObjectSummary(
        String id,
        String typeName,
        long shallowSizeBytes,
        long retainedSizeBytes,
        int inboundReferenceCount,
        int outboundReferenceCount,
        boolean retainedSizeAvailable) {

    public HeapDumpObjectSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(typeName, "typeName");
        if (shallowSizeBytes < 0 || retainedSizeBytes < 0
                || inboundReferenceCount < 0 || outboundReferenceCount < 0) {
            throw new IllegalArgumentException("heap dump sizes and reference counts must not be negative");
        }
    }
}
