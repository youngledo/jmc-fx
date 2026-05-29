package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.service.JmxMonitoringRepository;

public class FakeJmxMonitoringRepository implements JmxMonitoringRepository {

    private final Map<String, JmxAttributeSubscription> attributeSubscriptions = new LinkedHashMap<>();
    private final Map<String, List<JmxSubscriptionSample>> samples = new LinkedHashMap<>();
    private final Map<String, JmxNotificationSubscription> notificationSubscriptions = new LinkedHashMap<>();
    private final Map<String, List<JmxNotificationEvent>> notificationEvents = new LinkedHashMap<>();

    @Override
    public List<JmxAttributeSubscription> findAttributeSubscriptions(String connectionId) {
        return attributeSubscriptions.values().stream()
                .filter(subscription -> subscription.connectionId().equals(connectionId))
                .toList();
    }

    @Override
    public void saveAttributeSubscription(JmxAttributeSubscription subscription) {
        attributeSubscriptions.put(subscription.id(), subscription);
    }

    @Override
    public void deleteAttributeSubscription(String subscriptionId) {
        attributeSubscriptions.remove(subscriptionId);
        samples.remove(subscriptionId);
    }

    @Override
    public List<JmxSubscriptionSample> findSamples(String subscriptionId) {
        return List.copyOf(samples.getOrDefault(subscriptionId, List.of()));
    }

    @Override
    public void appendSample(JmxSubscriptionSample sample) {
        List<JmxSubscriptionSample> rows = samples.computeIfAbsent(sample.subscriptionId(), ignored -> new ArrayList<>());
        rows.add(sample);
        JmxAttributeSubscription subscription = attributeSubscriptions.get(sample.subscriptionId());
        if (subscription != null) {
            trimToNewest(rows, subscription.maxSamples());
        }
    }

    @Override
    public List<JmxNotificationSubscription> findNotificationSubscriptions(String connectionId) {
        return notificationSubscriptions.values().stream()
                .filter(subscription -> subscription.connectionId().equals(connectionId))
                .toList();
    }

    @Override
    public void saveNotificationSubscription(JmxNotificationSubscription subscription) {
        notificationSubscriptions.put(subscription.id(), subscription);
    }

    @Override
    public void deleteNotificationSubscription(String subscriptionId) {
        notificationSubscriptions.remove(subscriptionId);
        notificationEvents.remove(subscriptionId);
    }

    @Override
    public List<JmxNotificationEvent> findNotificationEvents(String subscriptionId) {
        return List.copyOf(notificationEvents.getOrDefault(subscriptionId, List.of()));
    }

    @Override
    public void appendNotificationEvent(JmxNotificationEvent event) {
        List<JmxNotificationEvent> events = notificationEvents
                .computeIfAbsent(event.subscriptionId(), ignored -> new ArrayList<>());
        events.add(event);
        JmxNotificationSubscription subscription = notificationSubscriptions.get(event.subscriptionId());
        if (subscription != null) {
            trimToNewest(events, subscription.maxEvents());
        }
    }

    private static <T> void trimToNewest(List<T> rows, int maxSize) {
        while (rows.size() > maxSize) {
            rows.removeFirst();
        }
    }
}
