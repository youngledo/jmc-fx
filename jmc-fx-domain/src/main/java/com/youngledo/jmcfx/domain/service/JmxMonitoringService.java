package com.youngledo.jmcfx.domain.service;

import java.util.List;
import java.util.function.Consumer;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface JmxMonitoringService {
    default JmxSubscriptionSample sampleAttribute(
            JvmConnection connection, JmxAttributeSubscription subscription) {
        throw new JmcFxException("JMX monitoring is not supported by this service.");
    }

    default List<JmxNotificationEvent> startNotifications(
            JvmConnection connection,
            JmxNotificationSubscription subscription,
            Consumer<JmxNotificationEvent> eventSink) {
        throw new JmcFxException("JMX notifications are not supported by this service.");
    }

    default void stopNotifications(JvmConnection connection, String subscriptionId) {
        throw new JmcFxException("JMX notifications are not supported by this service.");
    }
}
