package com.youngledo.jmcfx.domain.model;

public record VmOperationSummary(
        String operation,
        long count,
        long totalDurationMicros,
        long maxDurationMicros) {
}
