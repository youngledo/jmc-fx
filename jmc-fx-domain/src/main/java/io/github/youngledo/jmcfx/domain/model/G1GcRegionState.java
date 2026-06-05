package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record G1GcRegionState(
        long regionIndex,
        String type,
        String previousType,
        String eventKind,
        long usedBytes,
        long capacityBytes,
        long allocationContext,
        Instant startTime) {
}
