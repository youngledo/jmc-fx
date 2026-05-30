package com.youngledo.jmcfx.ui.jvms;

import java.time.Instant;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.LiveMetricKind;

public record LiveJvmOverviewMetric(
        String group,
        LiveMetricKind kind,
        String label,
        double value,
        String displayValue,
        String unit,
        Instant observedAt,
        long sequence) {

    public LiveJvmOverviewMetric {
        group = Objects.requireNonNullElse(group, "");
        kind = Objects.requireNonNull(kind, "kind");
        label = Objects.requireNonNullElse(label, "");
        displayValue = Objects.requireNonNullElse(displayValue, "");
        unit = Objects.requireNonNullElse(unit, "");
        observedAt = Objects.requireNonNullElse(observedAt, Instant.EPOCH);
    }
}
