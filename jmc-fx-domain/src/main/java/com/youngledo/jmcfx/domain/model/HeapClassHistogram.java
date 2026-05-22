package com.youngledo.jmcfx.domain.model;

public record HeapClassHistogram(
        String className,
        long instances,
        long size,
        long delta,
        double allocationPct) {
}
