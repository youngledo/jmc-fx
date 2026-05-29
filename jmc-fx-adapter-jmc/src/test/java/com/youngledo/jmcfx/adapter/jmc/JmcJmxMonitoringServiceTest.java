package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class JmcJmxMonitoringServiceTest {

    private static final JvmConnection CONNECTION = new JvmConnection("local", "Local", "", true);

    @Test
    void sampleAttributeReadsNumericMBeanAttribute() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Counter");
        server.registerMBean(new Counter(), name);
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> server,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "local", name.getCanonicalName(), "Count",
                "Count", "items", Duration.ofSeconds(1), 10, true, false);

        JmxSubscriptionSample sample = service.sampleAttribute(CONNECTION, subscription);

        assertEquals("sub-1", sample.subscriptionId());
        assertEquals(42.0, sample.numericValue());
        assertEquals("42", sample.displayValue());
        assertTrue(sample.numeric());
        assertEquals(Instant.parse("2026-05-29T00:00:00Z"), sample.observedAt());
    }

    @Test
    void startNotificationsRegistersListenerAndReturnsFutureEventsThroughSink() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Notifier");
        Notifier notifier = new Notifier();
        server.registerMBean(notifier, name);
        List<JmxNotificationEvent> events = new ArrayList<>();
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> server,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "local", name.getCanonicalName(), "Notifier", 10, true, false);

        service.startNotifications(CONNECTION, subscription, events::add);
        notifier.fire("changed");

        assertEquals(1, events.size());
        assertEquals("notif-1", events.getFirst().subscriptionId());
        assertEquals("jmcfx.changed", events.getFirst().type());
        assertEquals("changed", events.getFirst().message());
    }

    public interface CounterMBean {
        int getCount();
    }

    public static final class Counter implements CounterMBean {
        @Override
        public int getCount() {
            return 42;
        }
    }

    public interface NotifierMBean {
        void fire(String message);
    }

    public static final class Notifier extends NotificationBroadcasterSupport implements NotifierMBean {
        private long sequence;

        @Override
        public void fire(String message) {
            sendNotification(new Notification("jmcfx.changed", this, ++sequence, message));
        }
    }
}
