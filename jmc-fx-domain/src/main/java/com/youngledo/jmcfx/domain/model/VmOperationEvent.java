package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record VmOperationEvent(
        Instant startTime,
        String operation,
        boolean blocking,
        boolean safepoint,
        long durationMicros,
        String threadName) {
}
