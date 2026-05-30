package com.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.List;

public record G1GcReport(
        long snapshotCount,
        long transitionCount,
        long gcPauseCount,
        long regionCount,
        long usedBytes,
        long capacityBytes,
        Instant lastSnapshotTime,
        List<G1GcRegionSummary> regionSummaries,
        List<G1GcRegionState> recentRegionStates,
        List<GcEvent> gcPauses) {
}
