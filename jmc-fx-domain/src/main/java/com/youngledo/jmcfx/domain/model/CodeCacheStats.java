package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record CodeCacheStats(
        Instant startTime,
        String codeHeap,
        long entries,
        long methods,
        long adapters,
        long unallocated) {
}
