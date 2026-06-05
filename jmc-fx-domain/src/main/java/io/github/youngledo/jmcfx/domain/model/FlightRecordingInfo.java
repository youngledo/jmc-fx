package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record FlightRecordingInfo(
        long id,
        String name,
        FlightRecordingState state,
        long durationMillis,
        long sizeBytes) {

    public FlightRecordingInfo {
        name = Objects.requireNonNullElse(name, "");
        state = state == null ? FlightRecordingState.UNKNOWN : state;
    }
}
