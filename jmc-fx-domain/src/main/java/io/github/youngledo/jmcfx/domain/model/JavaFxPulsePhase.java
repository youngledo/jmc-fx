package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record JavaFxPulsePhase(
        long pulseId,
        String phaseName,
        long durationMicros,
        Instant startTime,
        String threadName) {
}
