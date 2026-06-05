package io.github.youngledo.jmcfx.domain.model;

public record GcHeapSummary(
        long gcId,
        String when,
        long heapUsed,
        long heapCommitted,
        long metaspaceUsed,
        long metaspaceCommitted,
        long metaspaceReserved) {
}
