package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationHistoryFilter;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationListeningState;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationListeningSummary;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.SavedJvmTarget;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.JmxConnectionService;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringService;
import io.github.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;
import org.junit.jupiter.api.Test;

class LiveJvmUseCasesTest {

    @Test
    void createsFacadeFromLiveJvmServices() {
        var services = new LiveJvmApplicationServices(
                new FakeJvmDiscoveryService(),
                new FakeJmxConnectionService(),
                null, null, null, null, null,
                new FakeJmxMonitoringService(),
                new FakeJmxMonitoringRepository(),
                new FakeSavedJvmTargetRepository(),
                new FakeJdpDiscoveryService());

        LiveJvmUseCases useCases = LiveJvmUseCases.from(services);

        assertNotNull(useCases.discovery());
        assertNotNull(useCases.connection());
        assertNotNull(useCases.monitoring());
        assertNotNull(useCases.persistence());
    }

    @Test
    void persistedEnabledNotificationSubscriptionStartsStoppedUntilRuntimeListenerStarts() {
        var repository = new FakeJmxMonitoringRepository();
        var useCase = new LiveJvmMonitoringUseCase(new FakeJmxMonitoringService(), repository);
        JvmConnection connection = connectedJvm("42");
        JmxNotificationSubscription subscription = notificationSubscription(connection, "notif-1", true);

        repository.saveNotificationSubscription(subscription);

        assertEquals(JmxNotificationListeningState.STOPPED,
                useCase.notificationListeningState(connection, subscription));
    }

    @Test
    void startAndStopNotificationsExposeRuntimeTransitionStates() {
        var repository = new FakeJmxMonitoringRepository();
        var service = new FakeJmxMonitoringService();
        var useCase = new LiveJvmMonitoringUseCase(service, repository);
        JvmConnection connection = connectedJvm("42");
        JmxNotificationSubscription subscription = notificationSubscription(connection, "notif-1", true);
        repository.saveNotificationSubscription(subscription);
        service.onStart = () -> assertEquals(JmxNotificationListeningState.STARTING,
                useCase.notificationListeningState(connection, subscription));
        service.onStop = () -> assertEquals(JmxNotificationListeningState.STOPPING,
                useCase.notificationListeningState(connection, subscription));

        useCase.startNotifications(connection, subscription, event -> { });

        assertEquals(JmxNotificationListeningState.LISTENING,
                useCase.notificationListeningState(connection, subscription));

        useCase.stopNotifications(connection, subscription.id());

        assertEquals(JmxNotificationListeningState.STOPPED,
                useCase.notificationListeningState(connection, subscription));
    }

    @Test
    void failedNotificationStartIsRetainedAsRuntimeState() {
        var service = new FakeJmxMonitoringService();
        service.failStart = true;
        var useCase = new LiveJvmMonitoringUseCase(service, new FakeJmxMonitoringRepository());
        JvmConnection connection = connectedJvm("42");
        JmxNotificationSubscription subscription = notificationSubscription(connection, "notif-1", true);

        assertThrows(JmcFxException.class, () -> useCase.startNotifications(connection, subscription, event -> { }));

        assertEquals(JmxNotificationListeningState.FAILED,
                useCase.notificationListeningState(connection, subscription));
    }

    @Test
    void unavailableMonitoringReportsUnavailableNotificationRuntimeState() {
        var useCase = new LiveJvmMonitoringUseCase(null, new FakeJmxMonitoringRepository());
        JvmConnection connection = connectedJvm("42");
        JmxNotificationSubscription subscription = notificationSubscription(connection, "notif-1", true);

        assertEquals(JmxNotificationListeningState.UNAVAILABLE,
                useCase.notificationListeningState(connection, subscription));
    }

    @Test
    void notificationListeningSummaryIsEmptyWhenRepositoryIsUnavailable() {
        var useCase = new LiveJvmMonitoringUseCase(new FakeJmxMonitoringService(), null);

        JmxNotificationListeningSummary summary = useCase.notificationListeningSummary("42");

        assertEquals(0, summary.total());
    }

    @Test
    void notificationListeningSummaryCountsRuntimeStatesForConnection() {
        var repository = new FakeJmxMonitoringRepository();
        var service = new FakeJmxMonitoringService();
        var useCase = new LiveJvmMonitoringUseCase(service, repository);
        JvmConnection connection = connectedJvm("42");
        JmxNotificationSubscription listening = notificationSubscription(connection, "notif-1", true);
        JmxNotificationSubscription stopped = notificationSubscription(connection, "notif-2", true);
        JmxNotificationSubscription failed = notificationSubscription(connection, "notif-3", true);
        repository.saveNotificationSubscription(listening);
        repository.saveNotificationSubscription(stopped);
        repository.saveNotificationSubscription(failed);
        useCase.startNotifications(connection, listening, event -> { });
        service.failStart = true;
        assertThrows(JmcFxException.class, () -> useCase.startNotifications(connection, failed, event -> { }));

        JmxNotificationListeningSummary summary = useCase.notificationListeningSummary(connection.id());

        assertEquals(1, summary.listening());
        assertEquals(1, summary.stopped());
        assertEquals(0, summary.starting());
        assertEquals(0, summary.stopping());
        assertEquals(1, summary.failed());
        assertEquals(0, summary.unavailable());
        assertEquals(3, summary.total());
    }

    @Test
    void filtersNotificationHistoryByTypeMessageAndTimeWindow() {
        var repository = new FakeJmxMonitoringRepository();
        var useCase = new LiveJvmMonitoringUseCase(new FakeJmxMonitoringService(), repository);
        JmxNotificationEvent matching = new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T01:00:00Z"),
                "memory.threshold", "heap", 1, "Heap high", "");
        JmxNotificationEvent wrongType = new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T01:01:00Z"),
                "thread.started", "worker", 2, "Heap high", "");
        JmxNotificationEvent wrongMessage = new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T01:02:00Z"),
                "memory.threshold", "heap", 3, "Metaspace changed", "");
        JmxNotificationEvent outsideWindow = new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T02:00:00Z"),
                "memory.threshold", "heap", 4, "Heap high", "");
        repository.appendNotificationEvent(matching);
        repository.appendNotificationEvent(wrongType);
        repository.appendNotificationEvent(wrongMessage);
        repository.appendNotificationEvent(outsideWindow);

        JmxNotificationHistoryFilter filter = new JmxNotificationHistoryFilter(
                "MEMORY", "heap", Instant.parse("2026-06-06T00:59:00Z"),
                Instant.parse("2026-06-06T01:30:00Z"));

        assertEquals(List.of(matching), useCase.findNotificationEvents("notif-1", filter));
    }

    private static JvmConnection connectedJvm(String id) {
        return new JvmConnection(id, "Test JVM", "service:jmx:rmi:///jndi/rmi://localhost:0/jmxrmi", true);
    }

    private static JmxNotificationSubscription notificationSubscription(
            JvmConnection connection, String id, boolean enabled) {
        return new JmxNotificationSubscription(
                id, connection.id(), "java.lang:type=Memory", "Memory", 100, enabled, true);
    }

    private static final class FakeJvmDiscoveryService implements JvmDiscoveryService {
        @Override
        public List<JvmConnection> discoverLocalJvms() {
            return List.of();
        }
    }

    private static final class FakeJmxConnectionService implements JmxConnectionService {
        @Override
        public JvmConnection connect(String connectionUrl) {
            return null;
        }

        @Override
        public void disconnect(JvmConnection connection) {
        }
    }

    private static final class FakeJmxMonitoringService implements JmxMonitoringService {
        private Runnable onStart = () -> { };
        private Runnable onStop = () -> { };
        private boolean failStart;

        @Override
        public List<JmxNotificationEvent> startNotifications(
                JvmConnection connection,
                JmxNotificationSubscription subscription,
                Consumer<JmxNotificationEvent> eventSink) {
            onStart.run();
            if (failStart) {
                throw new JmcFxException("start failed");
            }
            return List.of(new JmxNotificationEvent(
                    subscription.id(), Instant.EPOCH, "memory.threshold", "", 1, "", ""));
        }

        @Override
        public void stopNotifications(JvmConnection connection, String subscriptionId) {
            onStop.run();
        }
    }

    private static final class FakeJmxMonitoringRepository implements JmxMonitoringRepository {
        private final List<JmxNotificationSubscription> notificationSubscriptions = new ArrayList<>();
        private final List<JmxNotificationEvent> notificationEvents = new ArrayList<>();

        @Override
        public List<JmxNotificationSubscription> findNotificationSubscriptions(String connectionId) {
            return notificationSubscriptions.stream()
                    .filter(subscription -> subscription.connectionId().equals(connectionId))
                    .toList();
        }

        @Override
        public void saveNotificationSubscription(JmxNotificationSubscription subscription) {
            notificationSubscriptions.removeIf(saved -> saved.id().equals(subscription.id()));
            notificationSubscriptions.add(subscription);
        }

        @Override
        public List<JmxNotificationEvent> findNotificationEvents(String subscriptionId) {
            return notificationEvents.stream()
                    .filter(event -> event.subscriptionId().equals(subscriptionId))
                    .toList();
        }

        @Override
        public void appendNotificationEvent(JmxNotificationEvent event) {
            notificationEvents.add(event);
        }
    }

    private static final class FakeSavedJvmTargetRepository implements SavedJvmTargetRepository {
        @Override
        public List<SavedJvmTarget> findAll() {
            return List.of();
        }

        @Override
        public SavedJvmTarget save(SavedJvmTarget target) {
            return target;
        }

        @Override
        public void deleteById(String id) {
        }

        @Override
        public void markConnected(String id, java.time.Instant connectedAt) {
        }
    }

    private static final class FakeJdpDiscoveryService implements JdpDiscoveryService {
        @Override
        public List<io.github.youngledo.jmcfx.domain.model.JdpJvmAdvertisement> discover(java.time.Duration timeout) {
            return List.of();
        }
    }
}
