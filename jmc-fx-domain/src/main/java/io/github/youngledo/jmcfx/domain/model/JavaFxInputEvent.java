package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record JavaFxInputEvent(
        String inputType,
        long durationMicros,
        Instant startTime,
        String threadName) {
}
