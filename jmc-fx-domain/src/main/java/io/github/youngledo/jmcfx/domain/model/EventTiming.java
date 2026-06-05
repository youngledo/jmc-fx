package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record EventTiming(Instant startTime, Instant endTime, long durationNanos, String durationText,
        String recordingOffsetText) {
}
