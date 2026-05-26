package com.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record LiveMetricSnapshot(
        LiveMetricKind kind,
        double value,
        String unit,
        Instant observedAt) {

    public LiveMetricSnapshot {
        kind = Objects.requireNonNull(kind, "kind");
        unit = Objects.requireNonNullElse(unit, "");
        observedAt = Objects.requireNonNullElse(observedAt, Instant.EPOCH);
    }
}
