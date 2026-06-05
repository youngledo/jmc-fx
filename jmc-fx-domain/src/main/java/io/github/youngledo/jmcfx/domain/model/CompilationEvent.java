package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record CompilationEvent(
        long compilationId,
        String method,
        boolean succeeded,
        long durationMicros,
        long codeSize,
        long inlinedBytes,
        Instant startTime) {
}
