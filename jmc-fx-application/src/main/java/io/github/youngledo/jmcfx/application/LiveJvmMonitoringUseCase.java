package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringService;

public final class LiveJvmMonitoringUseCase {

    private final JmxMonitoringService service;
    private final JmxMonitoringRepository repository;

    public LiveJvmMonitoringUseCase(JmxMonitoringService service, JmxMonitoringRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    public boolean monitoringAvailable() {
        return service != null;
    }

    public boolean repositoryAvailable() {
        return repository != null;
    }

    public JmxSubscriptionSample sampleAttribute(JvmConnection connection, JmxAttributeSubscription subscription) {
        return service.sampleAttribute(connection, subscription);
    }

    public List<JmxNotificationEvent> startNotifications(
            JvmConnection connection,
            JmxNotificationSubscription subscription,
            Consumer<JmxNotificationEvent> eventSink) {
        return service.startNotifications(connection, subscription, eventSink);
    }

    public void stopNotifications(JvmConnection connection, String subscriptionId) {
        service.stopNotifications(connection, subscriptionId);
    }

    public List<JmxAttributeSubscription> findAttributeSubscriptions(String connectionId) {
        return repository.findAttributeSubscriptions(connectionId);
    }

    public void saveAttributeSubscription(JmxAttributeSubscription subscription) {
        repository.saveAttributeSubscription(subscription);
    }

    public void deleteAttributeSubscription(String subscriptionId) {
        repository.deleteAttributeSubscription(subscriptionId);
    }

    public List<JmxSubscriptionSample> findSamples(String subscriptionId) {
        return repository.findSamples(subscriptionId);
    }

    public void appendSample(JmxSubscriptionSample sample) {
        repository.appendSample(sample);
    }

    public List<JmxNotificationSubscription> findNotificationSubscriptions(String connectionId) {
        return repository.findNotificationSubscriptions(connectionId);
    }

    public void saveNotificationSubscription(JmxNotificationSubscription subscription) {
        repository.saveNotificationSubscription(subscription);
    }

    public void deleteNotificationSubscription(String subscriptionId) {
        repository.deleteNotificationSubscription(subscriptionId);
    }

    public List<JmxNotificationEvent> findNotificationEvents(String subscriptionId) {
        return repository.findNotificationEvents(subscriptionId);
    }

    public void appendNotificationEvent(JmxNotificationEvent event) {
        repository.appendNotificationEvent(event);
    }
}
