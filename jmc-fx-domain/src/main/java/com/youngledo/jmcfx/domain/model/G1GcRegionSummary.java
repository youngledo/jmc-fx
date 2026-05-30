package com.youngledo.jmcfx.domain.model;

public record G1GcRegionSummary(
        String type,
        long regionCount,
        long usedBytes,
        long capacityBytes) {
}
