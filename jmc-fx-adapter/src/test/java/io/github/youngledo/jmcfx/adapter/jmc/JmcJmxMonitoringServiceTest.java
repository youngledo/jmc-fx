package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
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

        List<JmxNotificationEvent> returnedEvents = service.startNotifications(CONNECTION, subscription, events::add);
        notifier.fire("changed");

        assertTrue(returnedEvents.isEmpty());
        assertEquals(1, events.size());
        assertEquals("notif-1", events.getFirst().subscriptionId());
        assertEquals("jmcfx.changed", events.getFirst().type());
        assertEquals("changed", events.getFirst().message());
    }

    @Test
    void startNotificationsReplacesExistingListenerForSameSubscription() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Notifier");
        Notifier notifier = new Notifier();
        server.registerMBean(notifier, name);
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> server,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "local", name.getCanonicalName(), "Notifier", 10, true, false);
        List<JmxNotificationEvent> firstSinkEvents = new ArrayList<>();
        List<JmxNotificationEvent> secondSinkEvents = new ArrayList<>();

        service.startNotifications(CONNECTION, subscription, firstSinkEvents::add);
        service.startNotifications(CONNECTION, subscription, secondSinkEvents::add);
        notifier.fire("changed");
        service.stopNotifications(CONNECTION, subscription.id());
        notifier.fire("ignored");

        assertTrue(firstSinkEvents.isEmpty());
        assertEquals(1, secondSinkEvents.size());
        assertEquals("changed", secondSinkEvents.getFirst().message());
    }

    @Test
    void stopNotificationsRemovesListenerAndPreventsFutureDelivery() throws Exception {
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
        service.stopNotifications(CONNECTION, subscription.id());
        notifier.fire("ignored");

        assertTrue(events.isEmpty());
    }

    @Test
    void stopNotificationsKeepsRegistryEntryWhenJmxRemovalFails() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Notifier");
        Notifier notifier = new Notifier();
        server.registerMBean(notifier, name);
        AtomicBoolean failRemoval = new AtomicBoolean(true);
        MBeanServer failingRemoveServer = failingRemoveServer(server, failRemoval);
        List<JmxNotificationEvent> events = new ArrayList<>();
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> failingRemoveServer,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "local", name.getCanonicalName(), "Notifier", 10, true, false);

        service.startNotifications(CONNECTION, subscription, events::add);
        assertThrows(JmcFxException.class, () -> service.stopNotifications(CONNECTION, subscription.id()));
        failRemoval.set(false);
        service.stopNotifications(CONNECTION, subscription.id());
        notifier.fire("ignored");

        assertTrue(events.isEmpty());
    }

    @Test
    void sampleAttributeDisplaysArrayValuesLikeMBeanBrowser() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Attributes");
        server.registerMBean(new Attributes(), name);
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> server,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "local", name.getCanonicalName(), "Names",
                "Names", "", Duration.ofSeconds(1), 10, true, false);

        JmxSubscriptionSample sample = service.sampleAttribute(CONNECTION, subscription);

        assertEquals("[alpha, beta]", sample.displayValue());
    }

    @Test
    void sampleAttributeMarksNonNumericValues() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Attributes");
        server.registerMBean(new Attributes(), name);
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> server,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "local", name.getCanonicalName(), "Status",
                "Status", "", Duration.ofSeconds(1), 10, true, false);

        JmxSubscriptionSample sample = service.sampleAttribute(CONNECTION, subscription);

        assertEquals("ready", sample.displayValue());
        assertTrue(Double.isNaN(sample.numericValue()));
        assertEquals(false, sample.numeric());
    }

    @Test
    void sampleAttributeWrapsJmxReadFailure() throws Exception {
        MBeanServer server = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("jmcfx.test:type=Attributes");
        server.registerMBean(new Attributes(), name);
        JmcJmxMonitoringService service = new JmcJmxMonitoringService(
                connection -> server,
                Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC));
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "local", name.getCanonicalName(), "Broken",
                "Broken", "", Duration.ofSeconds(1), 10, true, false);

        JmcFxException exception = assertThrows(
                JmcFxException.class, () -> service.sampleAttribute(CONNECTION, subscription));

        assertTrue(exception.getMessage().startsWith("Unable to sample JMX attribute:"));
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

    public interface AttributesMBean {
        String[] getNames();

        String getStatus();

        String getBroken();
    }

    public static final class Attributes implements AttributesMBean {
        @Override
        public String[] getNames() {
            return new String[] {"alpha", "beta"};
        }

        @Override
        public String getStatus() {
            return "ready";
        }

        @Override
        public String getBroken() {
            throw new IllegalStateException("boom");
        }
    }

    private static MBeanServer failingRemoveServer(MBeanServer server, AtomicBoolean failRemoval) {
        return (MBeanServer) Proxy.newProxyInstance(
                JmcJmxMonitoringServiceTest.class.getClassLoader(),
                new Class<?>[] {MBeanServer.class},
                (proxy, method, arguments) -> {
                    if ("removeNotificationListener".equals(method.getName())
                            && arguments != null
                            && arguments.length == 2
                            && arguments[1] instanceof NotificationListener
                            && failRemoval.get()) {
                        throw new IOException("remove failed");
                    }
                    return method.invoke(server, arguments);
                });
    }
}
