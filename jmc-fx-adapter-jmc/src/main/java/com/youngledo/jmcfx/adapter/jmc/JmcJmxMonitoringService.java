package com.youngledo.jmcfx.adapter.jmc;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.JmxMonitoringService;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class JmcJmxMonitoringService implements JmxMonitoringService {

    private final JmxConnectionAccessor connectionAccessor;
    private final Clock clock;
    private final ConcurrentMap<ListenerKey, RegisteredListener> listeners = new ConcurrentHashMap<>();

    public JmcJmxMonitoringService(JmxConnectionAccessor connectionAccessor) {
        this(connectionAccessor, Clock.systemUTC());
    }

    JmcJmxMonitoringService(JmxConnectionAccessor connectionAccessor, Clock clock) {
        this.connectionAccessor = Objects.requireNonNull(connectionAccessor, "connectionAccessor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public JmxSubscriptionSample sampleAttribute(JvmConnection connection, JmxAttributeSubscription subscription) {
        try {
            Object value = server(connection).getAttribute(new ObjectName(subscription.objectName()), subscription.attributeName());
            boolean numeric = value instanceof Number;
            return new JmxSubscriptionSample(
                    subscription.id(),
                    clock.instant(),
                    numeric ? ((Number) value).doubleValue() : Double.NaN,
                    String.valueOf(value),
                    subscription.unit(),
                    numeric);
        } catch (IOException | JMException | RuntimeException exception) {
            throw new JmcFxException("Unable to sample JMX attribute %s/%s: %s"
                    .formatted(subscription.objectName(), subscription.attributeName(), errorMessage(exception)), exception);
        }
    }

    @Override
    public List<JmxNotificationEvent> startNotifications(
            JvmConnection connection,
            JmxNotificationSubscription subscription,
            Consumer<JmxNotificationEvent> eventSink) {
        Objects.requireNonNull(eventSink, "eventSink");
        ListenerKey key = new ListenerKey(connection.id(), subscription.id());
        stopNotifications(connection, subscription.id());

        try {
            MBeanServerConnection server = server(connection);
            ObjectName objectName = new ObjectName(subscription.objectName());
            NotificationListener listener = (notification, handback) -> {
                JmxNotificationEvent event = notificationEvent(subscription, notification);
                eventSink.accept(event);
            };
            server.addNotificationListener(objectName, listener, null, null);
            listeners.put(key, new RegisteredListener(server, objectName, listener));
            return List.of();
        } catch (IOException | JMException | RuntimeException exception) {
            throw new JmcFxException("Unable to start JMX notifications for %s: %s"
                    .formatted(subscription.objectName(), errorMessage(exception)), exception);
        }
    }

    @Override
    public void stopNotifications(JvmConnection connection, String subscriptionId) {
        ListenerKey key = new ListenerKey(connection.id(), subscriptionId);
        RegisteredListener registration = listeners.remove(key);
        if (registration == null) {
            return;
        }

        try {
            registration.server().removeNotificationListener(registration.objectName(), registration.listener());
        } catch (IOException | JMException | RuntimeException exception) {
            throw new JmcFxException("Unable to stop JMX notifications for %s: %s"
                    .formatted(registration.objectName().getCanonicalName(), errorMessage(exception)), exception);
        }
    }

    private JmxNotificationEvent notificationEvent(
            JmxNotificationSubscription subscription, Notification notification) {
        Instant observedAt = clock.instant();
        return new JmxNotificationEvent(
                subscription.id(),
                observedAt,
                notification.getType(),
                Objects.toString(notification.getSource(), ""),
                notification.getSequenceNumber(),
                notification.getMessage(),
                Objects.toString(notification.getUserData(), ""));
    }

    private MBeanServerConnection server(JvmConnection connection) throws IOException {
        return connectionAccessor.mBeanServerConnection(connection);
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private record ListenerKey(String connectionId, String subscriptionId) {
    }

    private record RegisteredListener(
            MBeanServerConnection server,
            ObjectName objectName,
            NotificationListener listener) {
    }
}
