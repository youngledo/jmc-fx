package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record JmxSubscriptionSample(
        String subscriptionId,
        Instant observedAt,
        double numericValue,
        String displayValue,
        String unit,
        boolean numeric) {

    public JmxSubscriptionSample {
        subscriptionId = Objects.requireNonNullElse(subscriptionId, "");
        observedAt = Objects.requireNonNullElse(observedAt, Instant.EPOCH);
        displayValue = Objects.requireNonNullElse(displayValue, "");
        unit = Objects.requireNonNullElse(unit, "");
    }
}
