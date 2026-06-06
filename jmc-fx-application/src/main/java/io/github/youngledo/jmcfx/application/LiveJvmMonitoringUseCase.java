package io.github.youngledo.jmcfx.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationHistoryFilter;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationListeningState;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationListeningSummary;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringService;

public final class LiveJvmMonitoringUseCase {

    private final JmxMonitoringService service;
    private final JmxMonitoringRepository repository;
    private final Map<ListenerKey, JmxNotificationListeningState> notificationListeningStates =
            new ConcurrentHashMap<>();

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
        ListenerKey key = ListenerKey.of(connection, subscription.id());
        notificationListeningStates.put(key, JmxNotificationListeningState.STARTING);
        try {
            List<JmxNotificationEvent> events = service.startNotifications(connection, subscription, eventSink);
            notificationListeningStates.put(key, JmxNotificationListeningState.LISTENING);
            return events;
        } catch (RuntimeException exception) {
            notificationListeningStates.put(key, JmxNotificationListeningState.FAILED);
            throw exception;
        }
    }

    public void stopNotifications(JvmConnection connection, String subscriptionId) {
        ListenerKey key = ListenerKey.of(connection, subscriptionId);
        notificationListeningStates.put(key, JmxNotificationListeningState.STOPPING);
        try {
            service.stopNotifications(connection, subscriptionId);
            notificationListeningStates.put(key, JmxNotificationListeningState.STOPPED);
        } catch (RuntimeException exception) {
            notificationListeningStates.put(key, JmxNotificationListeningState.FAILED);
            throw exception;
        }
    }

    public JmxNotificationListeningState notificationListeningState(
            JvmConnection connection, JmxNotificationSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        return notificationListeningState(connection, subscription.id());
    }

    public JmxNotificationListeningState notificationListeningState(JvmConnection connection, String subscriptionId) {
        if (!monitoringAvailable()) {
            return JmxNotificationListeningState.UNAVAILABLE;
        }
        return notificationListeningStates.getOrDefault(ListenerKey.of(connection, subscriptionId),
                JmxNotificationListeningState.STOPPED);
    }

    public JmxNotificationListeningSummary notificationListeningSummary(String connectionId) {
        if (!repositoryAvailable()) {
            return JmxNotificationListeningSummary.from(List.of());
        }
        if (!monitoringAvailable()) {
            return JmxNotificationListeningSummary.from(findNotificationSubscriptions(connectionId).stream()
                    .map(subscription -> JmxNotificationListeningState.UNAVAILABLE)
                    .toList());
        }
        List<JmxNotificationListeningState> states = new ArrayList<>();
        for (JmxNotificationSubscription subscription : findNotificationSubscriptions(connectionId)) {
            states.add(notificationListeningState(connectionId, subscription.id()));
        }
        return JmxNotificationListeningSummary.from(states);
    }

    public List<JmxAttributeSubscription> findAttributeSubscriptions(String connectionId) {
        if (!repositoryAvailable()) {
            return List.of();
        }
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
        if (!repositoryAvailable()) {
            return List.of();
        }
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

    public List<JmxNotificationEvent> findNotificationEvents(
            String subscriptionId,
            JmxNotificationHistoryFilter filter) {
        List<JmxNotificationEvent> events = findNotificationEvents(subscriptionId);
        if (filter == null) {
            return events;
        }
        return events.stream()
                .filter(filter::matches)
                .toList();
    }

    public void appendNotificationEvent(JmxNotificationEvent event) {
        repository.appendNotificationEvent(event);
    }

    public void clearNotificationEvents(String subscriptionId) {
        repository.clearNotificationEvents(subscriptionId);
    }

    private JmxNotificationListeningState notificationListeningState(String connectionId, String subscriptionId) {
        return notificationListeningStates.getOrDefault(new ListenerKey(connectionId, subscriptionId),
                JmxNotificationListeningState.STOPPED);
    }

    private record ListenerKey(String connectionId, String subscriptionId) {

        private ListenerKey {
            connectionId = Objects.requireNonNullElse(connectionId, "");
            subscriptionId = Objects.requireNonNullElse(subscriptionId, "");
        }

        static ListenerKey of(JvmConnection connection, String subscriptionId) {
            Objects.requireNonNull(connection, "connection");
            return new ListenerKey(connection.id(), subscriptionId);
        }
    }
}
