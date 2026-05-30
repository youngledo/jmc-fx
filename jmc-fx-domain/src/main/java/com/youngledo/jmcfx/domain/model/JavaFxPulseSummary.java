package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record JavaFxPulseSummary(
        long pulseId,
        long phaseCount,
        long totalDurationMicros,
        long maxPhaseDurationMicros,
        Instant startTime) {
}
