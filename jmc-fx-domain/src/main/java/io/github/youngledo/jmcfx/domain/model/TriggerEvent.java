package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record TriggerEvent(
        String ruleId,
        String ruleName,
        LiveMetricKind metric,
        double value,
        String unit,
        Instant firedAt,
        String message) {

    public TriggerEvent {
        ruleId = Objects.requireNonNullElse(ruleId, "");
        ruleName = Objects.requireNonNullElse(ruleName, "");
        metric = Objects.requireNonNull(metric, "metric");
        unit = Objects.requireNonNullElse(unit, "");
        firedAt = Objects.requireNonNullElse(firedAt, Instant.EPOCH);
        message = Objects.requireNonNullElse(message, "");
    }
}
