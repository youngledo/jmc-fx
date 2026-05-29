package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.JmxMonitoringService;

public class FakeJmxMonitoringService implements JmxMonitoringService {

    private final Map<Key, JmxSubscriptionSample> samples = new HashMap<>();
    private final Map<Key, List<JmxNotificationEvent>> notificationEvents = new HashMap<>();
    private final List<String> stoppedNotificationIds = new ArrayList<>();

    public void setSample(String connectionId, String subscriptionId, JmxSubscriptionSample sample) {
        samples.put(new Key(connectionId, subscriptionId), sample);
    }

    public void setNotificationEvents(String connectionId, String subscriptionId, List<JmxNotificationEvent> events) {
        notificationEvents.put(new Key(connectionId, subscriptionId), List.copyOf(events));
    }

    public List<String> stoppedNotificationIds() {
        return List.copyOf(stoppedNotificationIds);
    }

    @Override
    public JmxSubscriptionSample sampleAttribute(JvmConnection connection, JmxAttributeSubscription subscription) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(subscription, "subscription");
        Key key = new Key(connection.id(), subscription.id());
        JmxSubscriptionSample sample = samples.get(key);
        if (sample == null) {
            throw new JmcFxException("No fake JMX sample for " + key.connectionId() + " " + key.subscriptionId());
        }
        return sample;
    }

    @Override
    public List<JmxNotificationEvent> startNotifications(
            JvmConnection connection,
            JmxNotificationSubscription subscription,
            Consumer<JmxNotificationEvent> eventSink) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(subscription, "subscription");
        Objects.requireNonNull(eventSink, "eventSink");
        Key key = new Key(connection.id(), subscription.id());
        List<JmxNotificationEvent> events = notificationEvents.get(key);
        if (events == null) {
            throw new JmcFxException("No fake JMX notification events for "
                    + key.connectionId() + " " + key.subscriptionId());
        }
        events.forEach(eventSink);
        return List.copyOf(events);
    }

    @Override
    public void stopNotifications(JvmConnection connection, String subscriptionId) {
        Objects.requireNonNull(connection, "connection");
        stoppedNotificationIds.add(Objects.requireNonNull(subscriptionId, "subscriptionId"));
    }

    private record Key(String connectionId, String subscriptionId) {
        private Key {
            connectionId = Objects.requireNonNullElse(connectionId, "");
            subscriptionId = Objects.requireNonNullElse(subscriptionId, "");
        }
    }
}
