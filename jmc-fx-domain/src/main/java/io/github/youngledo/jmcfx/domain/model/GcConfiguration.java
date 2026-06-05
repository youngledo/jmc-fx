package io.github.youngledo.jmcfx.domain.model;

public record GcConfiguration(
        String youngCollector,
        String oldCollector,
        long parallelGcThreads,
        long concurrentGcThreads,
        boolean explicitGcConcurrent,
        boolean explicitGcDisabled,
        boolean useDynamicGcThreads,
        long gcTimeRatio) {
}
