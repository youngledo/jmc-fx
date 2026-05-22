package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record ClassloadEvent(
        String eventType,
        Instant startTime,
        String loadedClass,
        String definingClassloader,
        String initiatingClassloader,
        long durationMicros) {
}
