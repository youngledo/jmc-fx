package io.github.youngledo.jmcfx.domain.model;

import java.nio.file.Path;
import java.time.Instant;

public record RecordingSummary(
        String id,
        Path path,
        String name,
        Instant startTime,
        Instant endTime,
        long durationMillis,
        long sizeBytes) {
}
