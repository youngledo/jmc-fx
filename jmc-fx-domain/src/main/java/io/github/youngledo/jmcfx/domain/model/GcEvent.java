package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record GcEvent(
        long gcId,
        String name,
        String cause,
        long longestPauseMicros,
        long totalPauseMicros,
        Instant startTime) {
}
