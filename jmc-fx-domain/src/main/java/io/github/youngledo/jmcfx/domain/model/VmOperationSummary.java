package io.github.youngledo.jmcfx.domain.model;

public record VmOperationSummary(
        String operation,
        long count,
        long totalDurationMicros,
        long maxDurationMicros) {
}
