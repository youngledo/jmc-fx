package io.github.youngledo.jmcfx.domain.model;

public record HeapUsage(
        long timestampEpochMillis,
        long usedHeap,
        long totalHeap) {
}
