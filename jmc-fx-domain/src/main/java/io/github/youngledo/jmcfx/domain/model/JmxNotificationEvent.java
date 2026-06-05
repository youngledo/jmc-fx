package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record JmxNotificationEvent(
        String subscriptionId,
        Instant observedAt,
        String type,
        String source,
        long sequenceNumber,
        String message,
        String userData) {

    public JmxNotificationEvent {
        subscriptionId = Objects.requireNonNullElse(subscriptionId, "");
        observedAt = Objects.requireNonNullElse(observedAt, Instant.EPOCH);
        type = Objects.requireNonNullElse(type, "");
        source = Objects.requireNonNullElse(source, "");
        message = Objects.requireNonNullElse(message, "");
        userData = Objects.requireNonNullElse(userData, "");
    }
}
