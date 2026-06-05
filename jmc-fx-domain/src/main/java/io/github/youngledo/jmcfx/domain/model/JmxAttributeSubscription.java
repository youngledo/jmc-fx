package io.github.youngledo.jmcfx.domain.model;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record JmxAttributeSubscription(
        String id,
        String connectionId,
        String objectName,
        String attributeName,
        String label,
        String unit,
        Duration samplingInterval,
        int maxSamples,
        boolean enabled,
        boolean persisted) {

    private static final Duration MIN_SAMPLING_INTERVAL = Duration.ofSeconds(1);
    private static final int DEFAULT_MAX_SAMPLES = 120;

    public JmxAttributeSubscription {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        connectionId = Objects.requireNonNullElse(connectionId, "");
        objectName = Objects.requireNonNullElse(objectName, "");
        attributeName = Objects.requireNonNullElse(attributeName, "");
        label = label == null || label.isBlank() ? attributeName : label;
        unit = Objects.requireNonNullElse(unit, "");
        samplingInterval = samplingInterval == null || samplingInterval.compareTo(MIN_SAMPLING_INTERVAL) < 0
                ? MIN_SAMPLING_INTERVAL
                : samplingInterval;
        maxSamples = maxSamples <= 0 ? DEFAULT_MAX_SAMPLES : maxSamples;
    }

    public JmxAttributeSubscription withEnabled(boolean enabled) {
        return new JmxAttributeSubscription(id, connectionId, objectName, attributeName, label, unit,
                samplingInterval, maxSamples, enabled, persisted);
    }
}
