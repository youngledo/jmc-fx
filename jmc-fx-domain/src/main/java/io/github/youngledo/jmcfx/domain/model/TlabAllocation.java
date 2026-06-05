package io.github.youngledo.jmcfx.domain.model;

public record TlabAllocation(
        String thread,
        long insideCount,
        long outsideCount,
        double insideAvgSize,
        double outsideAvgSize,
        long insideTotalSize,
        long outsideTotalSize) {
}
