package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record LiveMetricDefinition(
        LiveMetricKind kind,
        String label,
        String unit,
        double defaultThreshold) {

    public LiveMetricDefinition {
        kind = Objects.requireNonNull(kind, "kind");
        label = Objects.requireNonNullElse(label, "");
        unit = Objects.requireNonNullElse(unit, "");
    }
}
