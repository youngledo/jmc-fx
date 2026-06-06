package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxSubscriptionSample;

public interface JmxMonitoringRepository {
    default List<JmxAttributeSubscription> findAttributeSubscriptions(String connectionId) {
        return List.of();
    }

    default void saveAttributeSubscription(JmxAttributeSubscription subscription) {
    }

    default void deleteAttributeSubscription(String subscriptionId) {
    }

    default List<JmxSubscriptionSample> findSamples(String subscriptionId) {
        return List.of();
    }

    default void appendSample(JmxSubscriptionSample sample) {
    }

    default List<JmxNotificationSubscription> findNotificationSubscriptions(String connectionId) {
        return List.of();
    }

    default void saveNotificationSubscription(JmxNotificationSubscription subscription) {
    }

    default void deleteNotificationSubscription(String subscriptionId) {
    }

    default List<JmxNotificationEvent> findNotificationEvents(String subscriptionId) {
        return List.of();
    }

    default void appendNotificationEvent(JmxNotificationEvent event) {
    }

    default void clearNotificationEvents(String subscriptionId) {
    }
}
