package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;
import java.util.UUID;

public record JmxNotificationSubscription(
        String id,
        String connectionId,
        String objectName,
        String label,
        int maxEvents,
        boolean enabled,
        boolean persisted) {

    private static final int DEFAULT_MAX_EVENTS = 200;

    public JmxNotificationSubscription {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        connectionId = Objects.requireNonNullElse(connectionId, "");
        objectName = Objects.requireNonNullElse(objectName, "");
        label = label == null || label.isBlank() ? objectName : label;
        maxEvents = maxEvents <= 0 ? DEFAULT_MAX_EVENTS : maxEvents;
    }

    public JmxNotificationSubscription withEnabled(boolean enabled) {
        return new JmxNotificationSubscription(id, connectionId, objectName, label, maxEvents, enabled, persisted);
    }
}
