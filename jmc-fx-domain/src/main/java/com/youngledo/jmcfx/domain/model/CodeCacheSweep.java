package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record CodeCacheSweep(
        Instant startTime,
        long sweepIndex,
        long durationMicros,
        long flushed,
        long swept,
        long sweptCount) {
}
