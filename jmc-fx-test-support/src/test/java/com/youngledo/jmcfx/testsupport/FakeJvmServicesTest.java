package com.youngledo.jmcfx.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;
import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.model.JvmRuntimeSnapshot;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricKind;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.MemoryIssue;
import com.youngledo.jmcfx.domain.model.MemoryIssueCategory;
import com.youngledo.jmcfx.domain.model.MemoryIssueSeverity;
import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class FakeJvmServicesTest {

    @Test
    void fakeDiscoveryCanReplaceSnapshot() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(local("1", "one.Main")));
        discovery.setConnections(List.of(local("2", "two.Main")));

        assertEquals(1, discovery.discoverLocalJvms().size());
        assertEquals("2", discovery.discoverLocalJvms().getFirst().pid());
    }

    @Test
    void fakeSavedJvmTargetRepositoryStoresDeletesAndMarksConnected() {
        FakeSavedJvmTargetRepository repository = new FakeSavedJvmTargetRepository();
        SavedJvmTarget zeta = new SavedJvmTarget("zeta", "Zeta", "service:jmx:rmi:///zeta", null);
        SavedJvmTarget alphaA = new SavedJvmTarget("alpha-a", "Alpha", "service:jmx:rmi:///a", null);
        SavedJvmTarget alpha = new SavedJvmTarget("alpha", "alpha", "service:jmx:rmi:///alpha", null);

        repository.save(zeta);
        repository.save(alphaA);
        repository.save(alpha);
        repository.markConnected("zeta", Instant.EPOCH.plusSeconds(7));
        repository.deleteById("alpha");

        assertEquals(2, repository.findAll().size());
        assertEquals(alphaA, repository.findAll().getFirst());
        assertEquals(new SavedJvmTarget("zeta", "Zeta", "service:jmx:rmi:///zeta",
                Instant.EPOCH.plusSeconds(7)), repository.findAll().get(1));
    }

    @Test
    void fakeSavedJvmTargetRepositoryAssignsStableIdFromServiceUrl() {
        FakeSavedJvmTargetRepository repository = new FakeSavedJvmTargetRepository();
        SavedJvmTarget first = repository.save(new SavedJvmTarget("", "Demo",
                "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi", null));
        SavedJvmTarget second = repository.save(new SavedJvmTarget("", "Demo Updated",
                "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi", null));

        assertEquals(first.id(), second.id());
        assertEquals(List.of(second), repository.findAll());
    }

    @Test
    void fakeSavedJvmTargetRepositoryRejectsBlankServiceUrl() {
        FakeSavedJvmTargetRepository repository = new FakeSavedJvmTargetRepository();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.save(new SavedJvmTarget("", "Missing URL", " ", null)));

        assertEquals("Saved JVM target service URL must not be blank.", exception.getMessage());
    }

    @Test
    void fakeJdpDiscoveryReturnsConfiguredAdvertisementsAndRecordsTimeout() {
        FakeJdpDiscoveryService discovery = new FakeJdpDiscoveryService();
        JdpJvmAdvertisement alpha = advertisement("alpha", "Alpha", "service:jmx:rmi:///alpha");
        JdpJvmAdvertisement beta = advertisement("beta", "Beta", "service:jmx:rmi:///beta");

        discovery.add(alpha);
        discovery.setAdvertisements(List.of(beta, alpha));

        assertEquals(List.of(beta, alpha), discovery.discover(Duration.ofSeconds(3)));
        assertEquals(Duration.ofSeconds(3), discovery.lastTimeout());
    }

    @Test
    void fakeJdpDiscoveryThrowsConfiguredFailure() {
        FakeJdpDiscoveryService discovery = new FakeJdpDiscoveryService();
        RuntimeException failure = new RuntimeException("jdp offline");
        discovery.failWith(failure);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> discovery.discover(Duration.ofMillis(250)));

        assertEquals(failure, exception);
        assertEquals(Duration.ofMillis(250), discovery.lastTimeout());
    }

    @Test
    void fakeJmxServiceConnectsRemoteUrl() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JvmConnection connected = service.connect("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        assertTrue(connected.connected());
        assertEquals(JvmConnectionSource.MANUAL, connected.source());
        assertEquals(JvmConnectionState.CONNECTED, connected.state());
        assertTrue(service.connectedConnections().contains(connected.id()));
    }

    @Test
    void fakeJmxServiceConnectsLocalAttachableJvm() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JvmConnection connected = service.connectLocal(local("42", "demo.Main"));

        assertTrue(connected.connected());
        assertEquals("42", connected.id());
        assertEquals("42", connected.pid());
        assertEquals(JvmConnectionSource.LOCAL, connected.source());
        assertEquals("service:jmx:local://42", connected.connectionUrl());
        assertTrue(service.connectedConnections().contains(connected.id()));
    }

    @Test
    void fakeJmxServiceRejectsUnavailableLocalJvm() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.connectLocal(JvmConnection.local("42", "blocked.Main", "", false)));
    }

    @Test
    void fakeJmxServiceRejectsLocalJvmWithoutPid() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.connectLocal(JvmConnection.local("", "unknown.Main", "26.0.1", true)));
    }

    @Test
    void fakeJmxServiceReturnsRegisteredSessionSnapshot() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();
        JvmConnection connection = service.connect("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
        JvmSessionSnapshot snapshot = new JvmSessionSnapshot(connection,
                new JvmRuntimeSnapshot("OpenJDK 64-Bit Server VM", "Eclipse Adoptium",
                        "26.0.1", "26", java.time.Instant.EPOCH, 1000),
                List.of(new JvmCapabilitySnapshot(JvmCapability.MBEAN_SERVER,
                        JvmCapabilityStatus.AVAILABLE, "Available")));

        service.setSessionSnapshot(connection.id(), snapshot);

        assertEquals(snapshot, service.sessionSnapshot(connection));
    }

    @Test
    void fakeJmxServiceRejectsUnknownSessionSnapshot() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.sessionSnapshot(new JvmConnection("missing", "Missing", "", true)));

        assertEquals("No live JVM session for connection: missing", exception.getMessage());
    }

    @Test
    void fakeMBeanServiceReturnsRegisteredData() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanAttributeInfo vmName = new MBeanAttributeInfo("VmName", "java.lang.String", true, false,
                "OpenJDK", "");
        MBeanOperationInfo operation = new MBeanOperationInfo("gc", "void", "",
                List.of(new MBeanOperationParameter("verbose", "boolean", "")));

        service.setTree(connection.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        service.setAttributes(connection.id(), runtime.objectName(), List.of(vmName));
        service.setOperations(connection.id(), runtime.objectName(), List.of(operation));
        service.setOperationResult(connection.id(), runtime.objectName(), "gc", List.of("boolean"),
                new MBeanOperationResult(true, "ok", ""));

        assertEquals(1, service.tree(connection).size());
        assertEquals(vmName, service.attributes(connection, runtime.objectName()).getFirst());
        assertEquals(operation, service.operations(connection, runtime.objectName()).getFirst());
        assertEquals("ok", service.invoke(new MBeanOperationRequest(connection,
                runtime.objectName(), "gc", List.of("boolean"), List.of("true"))).value());
    }

    @Test
    void fakeMBeanServiceRejectsMissingObjectName() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.attributes(connection, "missing:type=Nope"));

        assertEquals("No fake MBean attributes for 42 missing:type=Nope", exception.getMessage());
    }

    @Test
    void fakeMBeanServiceRejectsWrongOperationSignature() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        String objectName = "demo:type=Operations";

        service.setOperationResult(connection.id(), objectName, "update", List.of("java.lang.String"),
                new MBeanOperationResult(true, "string", ""));

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.invoke(new MBeanOperationRequest(connection,
                        objectName, "update", List.of("int"), List.of("7"))));

        assertEquals("No fake MBean operation result for update", exception.getMessage());
    }

    @Test
    void fakeMBeanServiceResolvesSameNameOperationsBySignature() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        String objectName = "demo:type=Operations";

        service.setOperationResult(connection.id(), objectName, "update", List.of("java.lang.String"),
                new MBeanOperationResult(true, "string", ""));
        service.setOperationResult(connection.id(), objectName, "update", List.of("int"),
                new MBeanOperationResult(true, "int", ""));

        assertEquals("string", service.invoke(new MBeanOperationRequest(connection,
                objectName, "update", List.of("java.lang.String"), List.of("alpha"))).value());
        assertEquals("int", service.invoke(new MBeanOperationRequest(connection,
                objectName, "update", List.of("int"), List.of("7"))).value());
    }

    @Test
    void fakeJmxMonitoringServiceReturnsSamplesAndNotifications() {
        FakeJmxMonitoringService service = new FakeJmxMonitoringService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", connection.id(), "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(2), 10, true, false);
        JmxSubscriptionSample sample = new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH, 55.0, "55", "%", true);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", connection.id(), "demo:type=Notifier", "Notifier", 20, true, false);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo.type", "demo:type=Notifier", 1, "changed", "");

        service.setSample(connection.id(), subscription.id(), sample);
        service.setNotificationEvents(connection.id(), notificationSubscription.id(), List.of(event));

        assertEquals(sample, service.sampleAttribute(connection, subscription));
        assertEquals(List.of(event), service.startNotifications(connection, notificationSubscription, ignored -> { }));
        service.stopNotifications(connection, notificationSubscription.id());
        assertEquals(List.of(notificationSubscription.id()), service.stoppedNotificationIds());
    }

    @Test
    void fakeJmxMonitoringServiceRequiresConfiguredNotificationsAndNonNullArguments() {
        FakeJmxMonitoringService service = new FakeJmxMonitoringService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", connection.id(), "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(2), 10, true, false);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", connection.id(), "demo:type=Notifier", "Notifier", 20, true, false);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.startNotifications(connection, notificationSubscription, ignored -> { }));
        assertEquals("No fake JMX notification events for 42 notif-1", exception.getMessage());

        assertThrows(NullPointerException.class, () -> service.sampleAttribute(null, subscription));
        assertThrows(NullPointerException.class, () -> service.sampleAttribute(connection, null));
        assertThrows(NullPointerException.class,
                () -> service.startNotifications(null, notificationSubscription, ignored -> { }));
        assertThrows(NullPointerException.class,
                () -> service.startNotifications(connection, null, ignored -> { }));
        assertThrows(NullPointerException.class,
                () -> service.startNotifications(connection, notificationSubscription, null));
        assertThrows(NullPointerException.class, () -> service.stopNotifications(null, notificationSubscription.id()));
        assertThrows(NullPointerException.class, () -> service.stopNotifications(connection, null));

        service.setNotificationEvents(connection.id(), notificationSubscription.id(), List.of());
        assertEquals(List.of(), service.startNotifications(connection, notificationSubscription, ignored -> { }));
    }

    @Test
    void fakeJmxMonitoringRepositoryPersistsSubscriptionsAndEvents() {
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 3, true, true);
        JmxSubscriptionSample sample = new JmxSubscriptionSample("sub-1", Instant.EPOCH, 1.0, "1", "%", true);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", "42", "demo:type=Notifier", "Notifier", 2, true, true);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo.type", "demo:type=Notifier", 1, "changed", "");

        repository.saveAttributeSubscription(subscription);
        repository.appendSample(sample);
        repository.saveNotificationSubscription(notificationSubscription);
        repository.appendNotificationEvent(event);

        assertEquals(List.of(subscription), repository.findAttributeSubscriptions("42"));
        assertEquals(List.of(sample), repository.findSamples("sub-1"));
        assertEquals(List.of(notificationSubscription), repository.findNotificationSubscriptions("42"));
        assertEquals(List.of(event), repository.findNotificationEvents("notif-1"));

        assertThrows(UnsupportedOperationException.class,
                () -> repository.findAttributeSubscriptions("42").add(subscription));
        assertThrows(UnsupportedOperationException.class, () -> repository.findSamples("sub-1").add(sample));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findNotificationSubscriptions("42").add(notificationSubscription));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findNotificationEvents("notif-1").add(event));
    }

    @Test
    void fakeJmxMonitoringRepositoryDeletesSubscriptionsAndTheirEvents() {
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JmxAttributeSubscription attributeSubscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 3, true, true);
        JmxSubscriptionSample sample = new JmxSubscriptionSample("sub-1", Instant.EPOCH, 1.0, "1", "%", true);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", "42", "demo:type=Notifier", "Notifier", 2, true, true);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo.type", "demo:type=Notifier", 1, "changed", "");

        repository.saveAttributeSubscription(attributeSubscription);
        repository.appendSample(sample);
        repository.saveNotificationSubscription(notificationSubscription);
        repository.appendNotificationEvent(event);

        repository.deleteAttributeSubscription(attributeSubscription.id());
        repository.deleteNotificationSubscription(notificationSubscription.id());

        assertEquals(List.of(), repository.findAttributeSubscriptions("42"));
        assertEquals(List.of(), repository.findSamples("sub-1"));
        assertEquals(List.of(), repository.findNotificationSubscriptions("42"));
        assertEquals(List.of(), repository.findNotificationEvents("notif-1"));
    }

    @Test
    void fakeJmxMonitoringRepositoryKeepsNewestSamplesAndEventsWhenSubscriptionLimitsExist() {
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JmxAttributeSubscription attributeSubscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 2, true, true);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", "42", "demo:type=Notifier", "Notifier", 2, true, true);
        JmxSubscriptionSample firstSample = sample("sub-1", 1);
        JmxSubscriptionSample secondSample = sample("sub-1", 2);
        JmxSubscriptionSample thirdSample = sample("sub-1", 3);
        JmxNotificationEvent firstEvent = notificationEvent("notif-1", 1);
        JmxNotificationEvent secondEvent = notificationEvent("notif-1", 2);
        JmxNotificationEvent thirdEvent = notificationEvent("notif-1", 3);

        repository.saveAttributeSubscription(attributeSubscription);
        repository.appendSample(firstSample);
        repository.appendSample(secondSample);
        repository.appendSample(thirdSample);
        repository.saveNotificationSubscription(notificationSubscription);
        repository.appendNotificationEvent(firstEvent);
        repository.appendNotificationEvent(secondEvent);
        repository.appendNotificationEvent(thirdEvent);

        assertEquals(List.of(secondSample, thirdSample), repository.findSamples("sub-1"));
        assertEquals(List.of(secondEvent, thirdEvent), repository.findNotificationEvents("notif-1"));
    }

    @Test
    void fakeJmxMonitoringRepositoryDoesNotTrimSamplesAndEventsWithoutSavedSubscriptions() {
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JmxSubscriptionSample firstSample = sample("sub-1", 1);
        JmxSubscriptionSample secondSample = sample("sub-1", 2);
        JmxSubscriptionSample thirdSample = sample("sub-1", 3);
        JmxNotificationEvent firstEvent = notificationEvent("notif-1", 1);
        JmxNotificationEvent secondEvent = notificationEvent("notif-1", 2);
        JmxNotificationEvent thirdEvent = notificationEvent("notif-1", 3);

        repository.appendSample(firstSample);
        repository.appendSample(secondSample);
        repository.appendSample(thirdSample);
        repository.appendNotificationEvent(firstEvent);
        repository.appendNotificationEvent(secondEvent);
        repository.appendNotificationEvent(thirdEvent);

        assertEquals(List.of(firstSample, secondSample, thirdSample), repository.findSamples("sub-1"));
        assertEquals(List.of(firstEvent, secondEvent, thirdEvent), repository.findNotificationEvents("notif-1"));
    }

    @Test
    void fakeDiagnosticCommandServiceReturnsCommandsAndCapturesRequest() {
        FakeDiagnosticCommandService service = new FakeDiagnosticCommandService();
        JvmConnection connection = JvmConnection.local("42", "demo.Main", "26", true)
                .asConnected("service:jmx:local://42");
        DiagnosticCommandInfo command = new DiagnosticCommandInfo("threadPrint", "Thread Print", "",
                List.of());
        service.setCommands("42", List.of(command));
        service.setResult("42", "threadPrint", new DiagnosticCommandResult(true, "dump", ""));

        DiagnosticCommandResult result = service.execute(new DiagnosticCommandRequest(connection,
                "threadPrint", List.of("-l")));

        assertEquals(List.of(command), service.commands(connection));
        assertEquals("dump", result.output());
        assertEquals(List.of("-l"), service.lastRequest().arguments());
    }

    @Test
    void fakeLiveMetricServiceReturnsDefinitionsAndSnapshots() {
        FakeLiveMetricService service = new FakeLiveMetricService();
        JvmConnection connection = JvmConnection.local("42", "demo.Main", "26", true)
                .asConnected("service:jmx:local://42");
        LiveMetricDefinition definition = new LiveMetricDefinition(
                LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0);
        LiveMetricSnapshot snapshot = new LiveMetricSnapshot(
                LiveMetricKind.HEAP_USED_PERCENT, 91.5, "%", Instant.EPOCH);
        service.setDefinitions("42", List.of(definition));
        service.setSnapshot("42", List.of(snapshot));

        assertEquals(List.of(definition), service.definitions(connection));
        assertEquals(List.of(snapshot), service.snapshot(connection));
    }

    @Test
    void fakeAdvancedJfrAnalysisServiceReturnsConfiguredHeatmap() {
        FakeAdvancedJfrAnalysisService service = new FakeAdvancedJfrAnalysisService();
        EventHeatmap heatmap = new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1, List.of());
        service.setHeatmap(heatmap);

        assertEquals(heatmap, service.loadEventHeatmap(recording(), 1, 1));
    }

    @Test
    void fakeAdvancedJfrAnalysisServiceReturnsConfiguredMemoryAnalysisAndCapturesLimit() {
        FakeAdvancedJfrAnalysisService service = new FakeAdvancedJfrAnalysisService();
        MemoryIssue issue = new MemoryIssue(MemoryIssueCategory.ALLOCATION_HOTSPOT,
                MemoryIssueSeverity.WARNING, "byte[]", 64, 2, 50, "2 samples", "Review allocation rate.");
        MemoryAnalysisReport report = new MemoryAnalysisReport(64, 2, List.of(issue));
        service.setMemoryAnalysisReport(report);

        assertEquals(report, service.loadMemoryAnalysis(recording(), 12));
        assertEquals(12, service.lastMaxMemoryIssues());
    }

    private static JvmConnection local(String pid, String name) {
        return JvmConnection.local(pid, name, "26.0.1", true);
    }

    private static JdpJvmAdvertisement advertisement(String id, String name, String serviceUrl) {
        return new JdpJvmAdvertisement(id, name, serviceUrl, "localhost", 7091, "26.0.1");
    }

    private static com.youngledo.jmcfx.domain.model.RecordingSummary recording() {
        return new com.youngledo.jmcfx.domain.model.RecordingSummary("rec",
                java.nio.file.Path.of("sample.jfr"), "sample.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 1024);
    }

    private static JmxSubscriptionSample sample(String subscriptionId, int sequence) {
        return new JmxSubscriptionSample(subscriptionId, Instant.EPOCH.plusSeconds(sequence), sequence,
                Integer.toString(sequence), "%", true);
    }

    private static JmxNotificationEvent notificationEvent(String subscriptionId, long sequence) {
        return new JmxNotificationEvent(subscriptionId, Instant.EPOCH.plusSeconds(sequence), "demo.type",
                "demo:type=Notifier", sequence, "changed-" + sequence, "");
    }
}
