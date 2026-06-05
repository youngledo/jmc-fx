package io.github.youngledo.jmcfx.domain.model;

public record GcSummary(
        String generation,
        long collectionCount,
        long totalDurationMillis,
        double avgDurationMillis,
        long maxDurationMillis,
        long totalPauseMillis) {
}
