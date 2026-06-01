package com.youngledo.jmcfx.ui.jvms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingState;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.model.JvmRuntimeSnapshot;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;
import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentStatus;
import com.youngledo.jmcfx.domain.model.JmcAgentTransform;
import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricKind;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import com.youngledo.jmcfx.domain.model.TriggerActionType;
import com.youngledo.jmcfx.domain.model.TriggerEvent;
import com.youngledo.jmcfx.domain.model.TriggerOperator;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.testsupport.FakeDiagnosticCommandService;
import com.youngledo.jmcfx.testsupport.FakeFlightRecordingService;
import com.youngledo.jmcfx.testsupport.FakeJdpDiscoveryService;
import com.youngledo.jmcfx.testsupport.FakeJmcAgentService;
import com.youngledo.jmcfx.testsupport.FakeJmxConnectionService;
import com.youngledo.jmcfx.testsupport.FakeJmxMonitoringRepository;
import com.youngledo.jmcfx.testsupport.FakeJmxMonitoringService;
import com.youngledo.jmcfx.testsupport.FakeJvmDiscoveryService;
import com.youngledo.jmcfx.testsupport.FakeLiveMetricService;
import com.youngledo.jmcfx.testsupport.FakeMBeanBrowserService;
import com.youngledo.jmcfx.testsupport.FakeSavedJvmTargetRepository;

class JvmBrowserViewModelTest {

    @Test
    void refreshLoadsAttachableDiscoveredJvms() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());

        viewModel.refresh();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals("42", viewModel.connectionsProperty().getFirst().pid());
        assertEquals("demo.Main", viewModel.connectionsProperty().getFirst().displayName());
        assertEquals(JvmConnectionState.ATTACHABLE, viewModel.connectionsProperty().getFirst().state());
        assertFalse(viewModel.loadingProperty().get());
        assertFalse(viewModel.errorProperty().get());
        assertTrue(viewModel.refreshCompletedProperty().get());
        assertEquals("", viewModel.statusMessageProperty().get());
    }

    @Test
    void refreshCompletedOnlyAfterSuccessfulRefresh() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());

        assertFalse(viewModel.refreshCompletedProperty().get());

        viewModel.refresh();

        assertTrue(viewModel.refreshCompletedProperty().get());
        assertTrue(viewModel.connectionsProperty().isEmpty());
    }

    @Test
    void failedRefreshDoesNotMarkRefreshCompleted() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.failWith(new IllegalStateException("Discovery failed"));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());

        viewModel.refresh();

        assertFalse(viewModel.refreshCompletedProperty().get());
        assertTrue(viewModel.errorProperty().get());
    }

    @Test
    void expectedDomainFailureLogsWarningWithoutThrowable() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        jmx.failWith(new JmcFxException("target runtime lacks jdk.management.agent"));
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        RecordingAppender appender = attachRecordingAppender();
        try {
            viewModel.refresh();
            viewModel.connectSelectedOrManual();

            assertEquals("target runtime lacks jdk.management.agent", viewModel.errorMessageProperty().get());
            assertEquals(2, appender.events.size());
            assertEquals(Level.WARN, appender.events.getFirst().getLevel());
            assertEquals("JVM browser action failed: target runtime lacks jdk.management.agent",
                    appender.events.getFirst().getMessage().getFormattedMessage());
            assertNull(appender.events.getFirst().getThrown());
            assertEquals(Level.DEBUG, appender.events.getLast().getLevel());
            assertEquals("JVM browser action failed", appender.events.getLast().getMessage().getFormattedMessage());
            assertEquals(JmcFxException.class, appender.events.getLast().getThrown().getClass());
        } finally {
            detachRecordingAppender(appender);
        }
    }

    @Test
    void refreshDeduplicatesDiscoveredRowsByPid() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(localConnection("42", "demo.Main"),
                localConnection("42", "demo.Main duplicate")));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());

        viewModel.refresh();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals("42", viewModel.connectionsProperty().getFirst().pid());
    }

    @Test
    void loadingStaysTrueUntilAllOverlappingWorkFinishes() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(discovery, new FakeJmxConnectionService(),
                executor, Runnable::run);

        viewModel.refresh();
        viewModel.refresh();
        executor.runNext();

        assertTrue(viewModel.loadingProperty().get());

        executor.runNext();

        assertFalse(viewModel.loadingProperty().get());
    }

    @Test
    void refreshPreservesConnectedLocalRows() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(localConnection("42", "demo.Main")));
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        viewModel.refresh();
        viewModel.selectedConnectionProperty().set(viewModel.connectionsProperty().getFirst());
        viewModel.connectSelectedOrManual();

        discovery.setConnections(List.of(localConnection("42", "demo.Main updated"),
                localConnection("77", "other.Main")));
        viewModel.refresh();

        assertEquals(2, viewModel.connectionsProperty().size());
        assertEquals("demo.Main", viewModel.connectionsProperty().getFirst().displayName());
        assertEquals(JvmConnectionState.CONNECTED, viewModel.connectionsProperty().getFirst().state());
        assertEquals("77", viewModel.connectionsProperty().get(1).pid());
    }

    @Test
    void refreshRemovesOnlyUnconnectedLocalRowsThatDisappear() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(localConnection("42", "demo.Main"), localConnection("77", "other.Main")));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.refresh();
        viewModel.selectedConnectionProperty().set(viewModel.connectionsProperty().getFirst());
        viewModel.connectSelectedOrManual();

        discovery.setConnections(List.of(localConnection("42", "demo.Main")));
        viewModel.refresh();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals("42", viewModel.connectionsProperty().getFirst().pid());
        assertEquals(JvmConnectionState.CONNECTED, viewModel.connectionsProperty().getFirst().state());
    }

    @Test
    void refreshNeverRemovesManualRemoteConnections() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
        viewModel.connectSelectedOrManual();

        discovery.setConnections(List.of());
        viewModel.refresh();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals(JvmConnectionSource.MANUAL, viewModel.connectionsProperty().getFirst().source());
        assertTrue(viewModel.connectionsProperty().getFirst().connected());
    }

    @Test
    void refreshIncludesSavedTargetsWithoutDroppingConnectedManualOrJdpRows() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(localConnection("42", "demo.Main")));
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        savedTargets.save(new SavedJvmTarget("saved-1", "Production", "service:jmx:rmi:///prod", null));
        FakeJdpDiscoveryService jdp = new FakeJdpDiscoveryService();
        jdp.add(new JdpJvmAdvertisement("jdp-1", "Discovered", "service:jmx:rmi:///jdp", "host", 7091,
                "26.0.1"));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService(), savedTargets, jdp);
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///manual");
        viewModel.connectSelectedOrManual();
        viewModel.refreshJdp();

        viewModel.refresh();

        assertEquals(List.of(JvmConnectionSource.MANUAL, JvmConnectionSource.JDP, JvmConnectionSource.LOCAL,
                JvmConnectionSource.SAVED), viewModel.connectionsProperty().stream()
                        .map(JvmConnection::source)
                        .toList());
        assertTrue(viewModel.connectionsProperty().stream()
                .anyMatch(connection -> connection.source() == JvmConnectionSource.MANUAL && connection.connected()));
        assertTrue(viewModel.connectionsProperty().stream()
                .anyMatch(connection -> connection.source() == JvmConnectionSource.SAVED
                        && connection.id().equals("saved-1")));
    }

    @Test
    void saveManualTargetAddsSavedCandidateClearsNameAndRejectsBlankUrl() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                savedTargets, new FakeJdpDiscoveryService());
        viewModel.manualConnectionNameProperty().set(" Production ");
        viewModel.manualConnectionUrlProperty().set(" service:jmx:rmi:///prod ");

        viewModel.saveManualTarget();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals(JvmConnectionSource.SAVED, viewModel.connectionsProperty().getFirst().source());
        assertEquals("Production", viewModel.connectionsProperty().getFirst().displayName());
        assertEquals("", viewModel.manualConnectionNameProperty().get());
        assertEquals("", viewModel.manualConnectionUrlProperty().get());

        viewModel.manualConnectionUrlProperty().set(" ");
        viewModel.saveManualTarget();

        assertTrue(viewModel.errorProperty().get());
        assertEquals("Enter a JMX service URL to save.", viewModel.errorMessageProperty().get());
        assertEquals(1, savedTargets.findAll().size());
    }

    @Test
    void saveManualTargetUsesUrlAsNameWhenNameIsBlank() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                savedTargets, new FakeJdpDiscoveryService());
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///prod");

        viewModel.saveManualTarget();

        assertEquals("service:jmx:rmi:///prod", viewModel.connectionsProperty().getFirst().displayName());
        assertEquals("service:jmx:rmi:///prod", savedTargets.findAll().getFirst().displayName());
    }

    @Test
    void removeSelectedSavedTargetRemovesOnlyDisconnectedSavedTarget() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        savedTargets.save(new SavedJvmTarget("saved-1", "Production", "service:jmx:rmi:///prod", null));
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                savedTargets, new FakeJdpDiscoveryService());
        viewModel.refresh();
        JvmConnection saved = viewModel.connectionsProperty().getFirst();
        viewModel.selectedConnectionProperty().set(null);

        viewModel.removeSelectedSavedTarget();

        assertEquals(1, savedTargets.findAll().size());

        viewModel.selectedConnectionProperty().set(saved);
        viewModel.removeSelectedSavedTarget();

        assertTrue(viewModel.connectionsProperty().isEmpty());
        assertTrue(savedTargets.findAll().isEmpty());
    }

    @Test
    void removeSelectedSavedTargetDoesNotRemoveConnectedSavedTarget() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        savedTargets.save(new SavedJvmTarget("saved-1", "Production", "service:jmx:rmi:///prod", null));
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                savedTargets, new FakeJdpDiscoveryService());
        viewModel.refresh();
        viewModel.connectSelected();

        viewModel.removeSelectedSavedTarget();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals(1, savedTargets.findAll().size());
        assertTrue(viewModel.errorProperty().get());
        assertEquals("Select a disconnected saved JVM target to remove.", viewModel.errorMessageProperty().get());
    }

    @Test
    void refreshJdpAddsCandidatesRecordsTimeoutAndClearsProgress() {
        FakeJdpDiscoveryService jdp = new FakeJdpDiscoveryService();
        jdp.add(new JdpJvmAdvertisement("jdp-1", "Discovered", "service:jmx:rmi:///jdp", "host", 7091,
                "26.0.1"));
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                new FakeSavedJvmTargetRepository(), jdp);

        viewModel.refreshJdp();

        assertEquals(Duration.ofMillis(750), jdp.lastTimeout());
        assertFalse(viewModel.jdpRefreshInProgressProperty().get());
        assertEquals("Found 1 JDP target.", viewModel.jdpStatusMessageProperty().get());
        assertEquals(JvmConnectionSource.JDP, viewModel.connectionsProperty().getFirst().source());
    }

    @Test
    void refreshJdpHandlesNoServiceAndFailureAndClearsProgress() {
        JvmBrowserViewModel noService = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                new FakeSavedJvmTargetRepository(), null);

        noService.refreshJdp();

        assertFalse(noService.jdpRefreshInProgressProperty().get());
        assertEquals("JDP discovery is not configured.", noService.jdpStatusMessageProperty().get());

        FakeJdpDiscoveryService failingJdp = new FakeJdpDiscoveryService();
        failingJdp.failWith(new IllegalStateException("network unavailable"));
        JvmBrowserViewModel failing = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService(),
                new FakeSavedJvmTargetRepository(), failingJdp);

        failing.refreshJdp();

        assertFalse(failing.jdpRefreshInProgressProperty().get());
        assertEquals("JDP discovery failed: network unavailable", failing.jdpStatusMessageProperty().get());
        assertTrue(failing.connectionsProperty().isEmpty());
    }

    @Test
    void connectSelectedForSavedAndJdpUsesRemoteConnectAndMarksSavedTargetConnected() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        savedTargets.save(new SavedJvmTarget("saved-1", "Production", "service:jmx:rmi:///prod", null));
        FakeJdpDiscoveryService jdp = new FakeJdpDiscoveryService();
        jdp.add(new JdpJvmAdvertisement("jdp-1", "Discovered", "service:jmx:rmi:///jdp", "host", 7091,
                "26.0.1"));
        CapturingRemoteJmxConnectionService jmx = new CapturingRemoteJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, savedTargets, jdp);
        viewModel.refresh();
        viewModel.refreshJdp();
        JvmConnection saved = viewModel.connectionsProperty().stream()
                .filter(connection -> connection.source() == JvmConnectionSource.SAVED)
                .findFirst()
                .orElseThrow();

        viewModel.selectedConnectionProperty().set(saved);
        viewModel.connectSelected();

        assertEquals("service:jmx:rmi:///prod", jmx.remoteConnectionUrls.getFirst());
        assertTrue(savedTargets.findAll().getFirst().lastConnectedAt() != null);

        JvmConnection jdpConnection = viewModel.connectionsProperty().stream()
                .filter(connection -> connection.source() == JvmConnectionSource.JDP)
                .findFirst()
                .orElseThrow();
        viewModel.selectedConnectionProperty().set(jdpConnection);
        viewModel.connectSelected();

        assertEquals("service:jmx:rmi:///jdp", jmx.remoteConnectionUrls.getLast());
    }

    @Test
    void savedConnectionLoadsSessionThroughRemoteConnectionReturnedByService() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        savedTargets.save(new SavedJvmTarget("saved-1", "Production", "service:jmx:rmi:///prod", null));
        CapturingRemoteJmxConnectionService jmx = new CapturingRemoteJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, savedTargets,
                new FakeJdpDiscoveryService());
        viewModel.refresh();
        JvmConnection saved = viewModel.connectionsProperty().getFirst();
        JvmConnection live = new JvmConnection("remote-live", "Production", saved.connectionUrl(), true,
                JvmConnectionSource.MANUAL, JvmConnectionState.CONNECTED, "Connected");
        jmx.connectedToReturn = live;
        jmx.setSessionSnapshot("remote-live", sessionSnapshot(live));

        viewModel.connectSelected();

        assertEquals("OpenJDK 64-Bit Server VM", viewModel.selectedSessionProperty().get().runtime().vmName());
        assertEquals("saved-1", viewModel.selectedConnectionProperty().get().id());
    }

    @Test
    void saveManualTargetClearsUrlSoConnectSelectedOrManualUsesSelectedSavedTarget() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        CapturingRemoteJmxConnectionService jmx = new CapturingRemoteJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, savedTargets,
                new FakeJdpDiscoveryService());
        viewModel.manualConnectionNameProperty().set("Production");
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///prod");

        viewModel.saveManualTarget();
        viewModel.connectSelectedOrManual();

        assertEquals("", viewModel.manualConnectionUrlProperty().get());
        assertEquals(List.of("service:jmx:rmi:///prod"), jmx.remoteConnectionUrls);
        assertEquals(JvmConnectionSource.SAVED, viewModel.selectedConnectionProperty().get().source());
        assertTrue(savedTargets.findAll().getFirst().lastConnectedAt() != null);
    }

    @Test
    void selectedAttachableLocalJvmConnectsWithoutManualUrl() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        viewModel.refresh();

        viewModel.connectSelectedOrManual();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertTrue(viewModel.connectionsProperty().getFirst().connected());
        assertEquals(JvmConnectionState.CONNECTED, viewModel.connectionsProperty().getFirst().state());
        assertEquals("service:jmx:local://42", viewModel.connectionsProperty().getFirst().connectionUrl());
    }

    @Test
    void manualUrlTakesPriorityOverSelectedLocalJvm() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.refresh();
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        viewModel.connectSelectedOrManual();

        assertEquals(2, viewModel.connectionsProperty().size());
        assertEquals(JvmConnectionSource.MANUAL, viewModel.selectedConnectionProperty().get().source());
        assertEquals("", viewModel.manualConnectionUrlProperty().get());
    }

    @Test
    void selectedConnectIgnoresManualUrl() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.refresh();
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        viewModel.connectSelected();

        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals(JvmConnectionSource.LOCAL, viewModel.selectedConnectionProperty().get().source());
        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi",
                viewModel.manualConnectionUrlProperty().get());
    }

    @Test
    void selectedConnectDoesNotUseManualUrlForUnattachableSelection() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(JvmConnection.local("42", "blocked.Main", "", false));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.refresh();
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        viewModel.connectSelected();

        assertTrue(viewModel.errorProperty().get());
        assertEquals("Enter a JMX service URL or select an attachable JVM.", viewModel.errorMessageProperty().get());
        assertEquals(1, viewModel.connectionsProperty().size());
        assertEquals(JvmConnectionSource.LOCAL, viewModel.connectionsProperty().getFirst().source());
        assertFalse(viewModel.connectionsProperty().getFirst().connected());
    }

    @Test
    void connectRequiresManualUrlOrAttachableSelection() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(JvmConnection.local("42", "blocked.Main", "", false));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.refresh();

        viewModel.connectSelectedOrManual();

        assertTrue(viewModel.errorProperty().get());
        assertEquals("Enter a JMX service URL or select an attachable JVM.", viewModel.errorMessageProperty().get());
    }

    @Test
    void disconnectRequiresConnectedSelection() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        JvmBrowserViewModel viewModel = viewModel(discovery, new FakeJmxConnectionService());
        viewModel.refresh();

        viewModel.disconnectSelected();

        assertTrue(viewModel.errorProperty().get());
        assertEquals("Select a connected JVM to disconnect.", viewModel.errorMessageProperty().get());
        assertEquals(JvmConnectionState.ATTACHABLE, viewModel.connectionsProperty().getFirst().state());
    }

    @Test
    void disconnectSelectedUpdatesSelectedRow() {
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), new FakeJmxConnectionService());
        viewModel.manualConnectionUrlProperty().set("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
        viewModel.connectSelectedOrManual();
        viewModel.selectedConnectionProperty().set(viewModel.connectionsProperty().getFirst());

        viewModel.disconnectSelected();

        assertFalse(viewModel.connectionsProperty().getFirst().connected());
        assertEquals(JvmConnectionState.DISCONNECTED, viewModel.connectionsProperty().getFirst().state());
        assertEquals("Disconnected.", viewModel.statusMessageProperty().get());
    }

    @Test
    void connectSelectedLoadsSessionSnapshot() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        viewModel.refresh();
        JvmConnection selected = viewModel.connectionsProperty().getFirst();
        JvmSessionSnapshot snapshot = sessionSnapshot(selected.asConnected("service:jmx:local://42"));
        jmx.setSessionSnapshot("42", snapshot);

        viewModel.connectSelectedOrManual();

        assertEquals("OpenJDK 64-Bit Server VM",
                viewModel.selectedSessionProperty().get().runtime().vmName());
        assertFalse(viewModel.sessionLoadingProperty().get());
        assertFalse(viewModel.sessionErrorProperty().get());
    }

    @Test
    void selectingConnectedRowLoadsSessionSnapshot() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        JvmConnection connected = JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        jmx.setSessionSnapshot("42", sessionSnapshot(connected));
        viewModel.connectionsProperty().add(connected);

        viewModel.selectedConnectionProperty().set(connected);

        assertEquals(JvmCapabilityStatus.AVAILABLE,
                viewModel.selectedSessionProperty().get().statusOf(JvmCapability.MBEAN_SERVER));
    }

    @Test
    void sessionSnapshotFailurePreservesConnectedRow() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        viewModel.refresh();

        viewModel.connectSelectedOrManual();

        assertTrue(viewModel.connectionsProperty().getFirst().connected());
        assertTrue(viewModel.sessionErrorProperty().get());
        assertEquals("No live JVM session for connection: 42",
                viewModel.sessionErrorMessageProperty().get());
    }

    @Test
    void disconnectClearsSelectedSessionSnapshot() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.add(localConnection("42", "demo.Main"));
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(discovery, jmx);
        viewModel.refresh();
        JvmConnection selected = viewModel.connectionsProperty().getFirst();
        jmx.setSessionSnapshot("42", sessionSnapshot(selected.asConnected("service:jmx:local://42")));
        viewModel.connectSelectedOrManual();

        viewModel.disconnectSelected();

        assertEquals(null, viewModel.selectedSessionProperty().get());
        assertFalse(viewModel.sessionErrorProperty().get());
    }

    @Test
    void connectedSessionWithFlightRecorderLoadsRecordingControl() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(),
                jmx, recordings);
        JvmConnection connected = connectedWithFlightRecorder(viewModel, jmx, recordings);

        viewModel.selectedConnectionProperty().set(connected);

        assertTrue(viewModel.recordingControlAvailableProperty().get());
        assertEquals(1, viewModel.flightRecordingsProperty().size());
        assertEquals(FlightRecordingState.RUNNING, viewModel.flightRecordingsProperty().getFirst().state());
    }

    @Test
    void savedConnectionWithFlightRecorderStartsRecordingUsingLiveConnection() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        savedTargets.save(new SavedJvmTarget("saved-1", "Production", "service:jmx:rmi:///prod", null));
        CapturingRemoteJmxConnectionService jmx = new CapturingRemoteJmxConnectionService();
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, recordings,
                null, null, null, savedTargets, new FakeJdpDiscoveryService(), new DirectJvmBrowserExecutor(),
                Runnable::run, path -> { });
        viewModel.refresh();
        JvmConnection saved = viewModel.connectionsProperty().getFirst();
        JvmConnection live = new JvmConnection("remote-live", "Production", saved.connectionUrl(), true,
                JvmConnectionSource.MANUAL, JvmConnectionState.CONNECTED, "Connected");
        jmx.connectedToReturn = live;
        jmx.setSessionSnapshot("remote-live", flightRecorderSessionSnapshot(live));
        recordings.setAvailable("remote-live", true);

        viewModel.connectSelected();
        viewModel.startFlightRecording();

        assertEquals("remote-live", recordings.lastStartRequest().connection().id());
        assertEquals(1, viewModel.flightRecordingsProperty().size());
        assertEquals(FlightRecordingState.RUNNING, viewModel.flightRecordingsProperty().getFirst().state());
    }

    @Test
    void connectedSessionShowsStoppedFlightRecordings() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(),
                jmx, recordings);
        JvmConnection connected = connectedWithFlightRecorder(viewModel, jmx, recordings);
        recordings.addRecording("42", new FlightRecordingInfo(101, "Stopped",
                FlightRecordingState.STOPPED, 1_000, 4096));

        viewModel.selectedConnectionProperty().set(connected);

        assertEquals(2, viewModel.flightRecordingsProperty().size());
        assertEquals(FlightRecordingState.RUNNING, viewModel.flightRecordingsProperty().getFirst().state());
        assertEquals(FlightRecordingState.STOPPED, viewModel.flightRecordingsProperty().getLast().state());
    }

    @Test
    void startRecordingAddsRunningRecording() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(),
                jmx, recordings);
        viewModel.selectedConnectionProperty().set(connectedWithFlightRecorder(viewModel, jmx, recordings));

        viewModel.startFlightRecording();

        assertEquals(2, viewModel.flightRecordingsProperty().size());
        assertEquals(FlightRecordingState.RUNNING, viewModel.flightRecordingsProperty().getLast().state());
        assertTrue(recordings.lastStartRequest().name().matches("jmcfx-42-\\d{14}"));
        assertEquals("", viewModel.recordingStatusMessageProperty().get());
    }

    @Test
    void stopAndSaveRecordingPublishesSavedFileForOpening() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        List<Path> opened = new ArrayList<>();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(),
                jmx, recordings, new DirectJvmBrowserExecutor(), Runnable::run, opened::add);
        viewModel.selectedConnectionProperty().set(connectedWithFlightRecorder(viewModel, jmx, recordings));
        viewModel.selectedFlightRecordingProperty().set(viewModel.flightRecordingsProperty().getFirst());

        viewModel.stopAndSaveSelectedFlightRecording(Path.of("target/live-capture.jfr"));

        assertEquals(List.of(Path.of("target/live-capture.jfr")), opened);
        assertEquals(100, recordings.lastStopRequest().recordingId());
        assertEquals("", viewModel.recordingStatusMessageProperty().get());
    }

    @Test
    void stopAndSaveRecordingRemovesSavedRecordingFromVisibleList() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(),
                jmx, recordings);
        viewModel.selectedConnectionProperty().set(connectedWithFlightRecorder(viewModel, jmx, recordings));
        viewModel.selectedFlightRecordingProperty().set(viewModel.flightRecordingsProperty().getFirst());

        viewModel.stopAndSaveSelectedFlightRecording(Path.of("target/Existing.jfr"));

        assertEquals(List.of(), viewModel.flightRecordingsProperty());
    }

    @Test
    void closeDiscardsOnlyRecordingsStartedInThisSession() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, recordings);
        viewModel.selectedConnectionProperty().set(connectedWithFlightRecorder(viewModel, jmx, recordings));

        viewModel.startFlightRecording();
        long startedRecordingId = viewModel.flightRecordingsProperty().getLast().id();

        viewModel.close();

        assertEquals(List.of(startedRecordingId), recordings.discardedRecordingIds());
    }

    @Test
    void closeDoesNotDiscardSessionRecordingAfterItWasSaved() {
        FakeFlightRecordingService recordings = new FakeFlightRecordingService();
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, recordings);
        viewModel.selectedConnectionProperty().set(connectedWithFlightRecorder(viewModel, jmx, recordings));
        viewModel.startFlightRecording();
        viewModel.selectedFlightRecordingProperty().set(viewModel.flightRecordingsProperty().getLast());

        viewModel.stopAndSaveSelectedFlightRecording(Path.of("target/live-capture.jfr"));
        viewModel.close();

        assertEquals(List.of(), recordings.discardedRecordingIds());
    }

    @Test
    void connectedSessionWithMBeanServerLoadsMBeanTree() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));

        viewModel.selectedConnectionProperty().set(connected);

        assertTrue(viewModel.mbeanBrowserAvailableProperty().get());
        assertEquals(1, viewModel.mbeanTreeProperty().size());
        assertEquals("java.lang", viewModel.mbeanTreeProperty().getFirst().name());
        assertFalse(viewModel.mbeanLoadingProperty().get());
        assertFalse(viewModel.mbeanErrorProperty().get());
    }

    @Test
    void selectingMBeanObjectLoadsAttributesAndOperations() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanAttributeInfo vmName = new MBeanAttributeInfo("VmName", "java.lang.String", true, false,
                "OpenJDK", "");
        MBeanOperationInfo gc = new MBeanOperationInfo("gc", "void", "", List.of());
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(vmName));
        mbeans.setOperations(connected.id(), runtime.objectName(), List.of(gc));
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.selectedMBeanProperty().set(runtime);

        assertEquals(List.of(vmName), viewModel.mbeanAttributesProperty());
        assertEquals(List.of(gc), viewModel.mbeanOperationsProperty());
        assertEquals(gc, viewModel.selectedMBeanOperationProperty().get());
    }

    @Test
    void refreshReloadsSelectedMBeanAttributes() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanAttributeInfo oldValue = new MBeanAttributeInfo("Uptime", "long", true, false, "1", "");
        MBeanAttributeInfo newValue = new MBeanAttributeInfo("Uptime", "long", true, false, "2", "");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(oldValue));
        mbeans.setOperations(connected.id(), runtime.objectName(), List.of());
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(runtime);
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(newValue));

        viewModel.refreshSelectedMBeanAttributes();

        assertEquals(List.of(newValue), viewModel.mbeanAttributesProperty());
    }

    @Test
    void invokeSelectedOperationStoresResultAndUsesParameterTypes() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        CapturingMBeanBrowserService mbeans = new CapturingMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode operations = MBeanNode.objectName("demo:type=Operations", "Operations");
        MBeanOperationInfo update = new MBeanOperationInfo("update", "java.lang.String", "",
                List.of(new MBeanOperationParameter("name", "java.lang.String", ""),
                        new MBeanOperationParameter("count", "int", "")));
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("demo", List.of(operations))));
        mbeans.setAttributes(connected.id(), operations.objectName(), List.of());
        mbeans.setOperations(connected.id(), operations.objectName(), List.of(update));
        mbeans.setOperationResult(connected.id(), operations.objectName(), "update",
                List.of("java.lang.String", "int"), new MBeanOperationResult(true, "updated", ""));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(operations);
        viewModel.mbeanOperationArgumentsProperty().set(" alpha , 7 ");

        viewModel.invokeSelectedMBeanOperation();

        assertEquals("updated", viewModel.mbeanOperationResultProperty().get());
        assertEquals(List.of("java.lang.String", "int"), mbeans.lastRequest.parameterTypes());
        assertEquals(List.of("alpha", "7"), mbeans.lastRequest.arguments());
    }

    @Test
    void failedInvokeSetsMBeanError() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode operations = MBeanNode.objectName("demo:type=Operations", "Operations");
        MBeanOperationInfo update = new MBeanOperationInfo("update", "void", "", List.of());
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("demo", List.of(operations))));
        mbeans.setAttributes(connected.id(), operations.objectName(), List.of());
        mbeans.setOperations(connected.id(), operations.objectName(), List.of(update));
        mbeans.setOperationResult(connected.id(), operations.objectName(), "update",
                new MBeanOperationResult(false, "", "rejected"));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(operations);

        viewModel.invokeSelectedMBeanOperation();

        assertTrue(viewModel.mbeanErrorProperty().get());
        assertEquals("rejected", viewModel.mbeanErrorMessageProperty().get());
        assertEquals("rejected", viewModel.mbeanOperationResultProperty().get());
    }

    @Test
    void selectingMBeanDomainNodeClearsDetailsAndDoesNotFail() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanNode domain = MBeanNode.domain("java.lang", List.of(runtime));
        mbeans.setTree(connected.id(), List.of(domain));
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(
                new MBeanAttributeInfo("VmName", "java.lang.String", true, false, "OpenJDK", "")));
        mbeans.setOperations(connected.id(), runtime.objectName(), List.of(
                new MBeanOperationInfo("gc", "void", "", List.of())));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(runtime);

        viewModel.selectedMBeanProperty().set(domain);

        assertTrue(viewModel.mbeanAttributesProperty().isEmpty());
        assertTrue(viewModel.mbeanOperationsProperty().isEmpty());
        assertEquals(null, viewModel.selectedMBeanOperationProperty().get());
        assertFalse(viewModel.mbeanErrorProperty().get());
    }

    @Test
    void disconnectClearsMBeanState() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(
                new MBeanAttributeInfo("VmName", "java.lang.String", true, false, "OpenJDK", "")));
        mbeans.setOperations(connected.id(), runtime.objectName(), List.of(
                new MBeanOperationInfo("gc", "void", "", List.of())));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(runtime);

        viewModel.disconnectSelected();

        assertTrue(viewModel.mbeanTreeProperty().isEmpty());
        assertTrue(viewModel.mbeanAttributesProperty().isEmpty());
        assertTrue(viewModel.mbeanOperationsProperty().isEmpty());
        assertEquals(null, viewModel.selectedMBeanProperty().get());
        assertFalse(viewModel.mbeanBrowserAvailableProperty().get());
        assertFalse(viewModel.mbeanLoadingProperty().get());
        assertFalse(viewModel.mbeanErrorProperty().get());
    }

    @Test
    void staleMBeanTreeFailureAfterDisconnectDoesNotSetError() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        mbeans.failWith(new IllegalStateException("stale tree"));
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, mbeans,
                executor, Runnable::run);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        viewModel.selectedConnectionProperty().set(connected);
        executor.runNext();

        viewModel.disconnectSelected();
        executor.runLast();
        executor.runNext();

        assertFalse(viewModel.mbeanErrorProperty().get());
        assertEquals("", viewModel.mbeanErrorMessageProperty().get());
        assertFalse(viewModel.mbeanLoadingProperty().get());
    }

    @Test
    void selectingDomainWhileMBeanDetailsLoadIsInFlightIgnoresStaleCompletion() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, mbeans,
                executor, Runnable::run);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanNode domain = MBeanNode.domain("java.lang", List.of(runtime));
        MBeanAttributeInfo vmName = new MBeanAttributeInfo("VmName", "java.lang.String", true, false,
                "OpenJDK", "");
        MBeanOperationInfo gc = new MBeanOperationInfo("gc", "void", "", List.of());
        mbeans.setTree(connected.id(), List.of(domain));
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(vmName));
        mbeans.setOperations(connected.id(), runtime.objectName(), List.of(gc));
        viewModel.selectedConnectionProperty().set(connected);
        executor.runNext();
        executor.runNext();
        viewModel.selectedMBeanProperty().set(runtime);

        viewModel.selectedMBeanProperty().set(domain);
        executor.runNext();

        assertFalse(viewModel.mbeanLoadingProperty().get());
        assertTrue(viewModel.mbeanAttributesProperty().isEmpty());
        assertTrue(viewModel.mbeanOperationsProperty().isEmpty());
        assertFalse(viewModel.mbeanErrorProperty().get());
    }

    @Test
    void staleSessionSnapshotSuccessAfterSelectionClearDoesNotReloadMBeans() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, mbeans,
                executor, Runnable::run);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.selectedConnectionProperty().set(null);
        executor.runNext();

        assertEquals(null, viewModel.selectedSessionProperty().get());
        assertTrue(viewModel.mbeanTreeProperty().isEmpty());
        assertFalse(viewModel.mbeanBrowserAvailableProperty().get());
        assertFalse(viewModel.mbeanLoadingProperty().get());
        assertTrue(executor.isEmpty());
    }

    @Test
    void switchingConnectedSelectionClearsMBeanStateBeforeNewSessionLoads() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        CapturingMBeanBrowserService mbeans = new CapturingMBeanBrowserService();
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, mbeans,
                executor, Runnable::run);
        JvmConnection first = connectedWithMBeans(viewModel, jmx, "42");
        JvmConnection second = connectedWithMBeans(viewModel, jmx, "84");
        MBeanNode operations = MBeanNode.objectName("demo:type=Operations", "Operations");
        MBeanOperationInfo update = new MBeanOperationInfo("update", "void", "", List.of());
        mbeans.setTree(first.id(), List.of(MBeanNode.domain("demo", List.of(operations))));
        mbeans.setAttributes(first.id(), operations.objectName(), List.of());
        mbeans.setOperations(first.id(), operations.objectName(), List.of(update));
        mbeans.setOperationResult(first.id(), operations.objectName(), "update",
                new MBeanOperationResult(true, "old", ""));
        viewModel.selectedConnectionProperty().set(first);
        executor.runNext();
        executor.runNext();
        viewModel.selectedMBeanProperty().set(operations);
        executor.runNext();

        viewModel.selectedConnectionProperty().set(second);
        viewModel.invokeSelectedMBeanOperation();

        assertEquals(null, viewModel.selectedSessionProperty().get());
        assertTrue(viewModel.mbeanTreeProperty().isEmpty());
        assertTrue(viewModel.mbeanAttributesProperty().isEmpty());
        assertTrue(viewModel.mbeanOperationsProperty().isEmpty());
        assertEquals(null, viewModel.selectedMBeanProperty().get());
        assertEquals(null, viewModel.selectedMBeanOperationProperty().get());
        assertFalse(viewModel.mbeanBrowserAvailableProperty().get());
        assertEquals(null, mbeans.lastRequest);
        assertEquals("Select an MBean operation to invoke.", viewModel.mbeanErrorMessageProperty().get());
    }

    @Test
    void failingMBeanSelectionClearsPreviousDetailsBeforeInvokeCanUseThem() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        CapturingMBeanBrowserService mbeans = new CapturingMBeanBrowserService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode first = MBeanNode.objectName("demo:type=First", "First");
        MBeanNode second = MBeanNode.objectName("demo:type=Second", "Second");
        MBeanOperationInfo update = new MBeanOperationInfo("update", "void", "", List.of());
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("demo", List.of(first, second))));
        mbeans.setAttributes(connected.id(), first.objectName(), List.of());
        mbeans.setOperations(connected.id(), first.objectName(), List.of(update));
        mbeans.setOperationResult(connected.id(), first.objectName(), "update",
                new MBeanOperationResult(true, "old", ""));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(first);
        mbeans.failWith(new IllegalStateException("new details failed"));

        viewModel.selectedMBeanProperty().set(second);
        viewModel.invokeSelectedMBeanOperation();

        assertTrue(viewModel.mbeanAttributesProperty().isEmpty());
        assertTrue(viewModel.mbeanOperationsProperty().isEmpty());
        assertEquals(null, viewModel.selectedMBeanOperationProperty().get());
        assertEquals(null, mbeans.lastRequest);
        assertEquals("Select an MBean operation to invoke.", viewModel.mbeanErrorMessageProperty().get());
    }

    @Test
    void operationSelectionChangeWhileInvokeIsInFlightIgnoresStaleInvokeResult() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, mbeans,
                executor, Runnable::run);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode operations = MBeanNode.objectName("demo:type=Operations", "Operations");
        MBeanOperationInfo update = new MBeanOperationInfo("update", "void", "", List.of());
        MBeanOperationInfo reset = new MBeanOperationInfo("reset", "void", "", List.of());
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("demo", List.of(operations))));
        mbeans.setAttributes(connected.id(), operations.objectName(), List.of());
        mbeans.setOperations(connected.id(), operations.objectName(), List.of(update, reset));
        mbeans.setOperationResult(connected.id(), operations.objectName(), "update",
                new MBeanOperationResult(false, "", "old failure"));
        viewModel.selectedConnectionProperty().set(connected);
        executor.runNext();
        executor.runNext();
        viewModel.selectedMBeanProperty().set(operations);
        executor.runNext();
        viewModel.selectedMBeanOperationProperty().set(update);
        viewModel.invokeSelectedMBeanOperation();

        viewModel.selectedMBeanOperationProperty().set(reset);
        executor.runNext();

        assertEquals("", viewModel.mbeanOperationResultProperty().get());
        assertFalse(viewModel.mbeanErrorProperty().get());
        assertEquals("", viewModel.mbeanErrorMessageProperty().get());
    }

    @Test
    void connectedSessionWithDiagnosticCommandsLoadsCommands() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeDiagnosticCommandService diagnostics = new FakeDiagnosticCommandService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, diagnostics);
        JvmConnection connected = connectedWithDiagnosticCommands(viewModel, jmx);
        DiagnosticCommandInfo vmCommandLine = new DiagnosticCommandInfo("VM.command_line",
                "VM Command Line", "Prints the command line.", List.of());
        diagnostics.setCommands(connected.id(), List.of(vmCommandLine));

        viewModel.selectedConnectionProperty().set(connected);

        assertTrue(viewModel.diagnosticCommandsAvailableProperty().get());
        assertEquals(List.of(vmCommandLine), viewModel.diagnosticCommandsProperty());
        assertEquals(vmCommandLine, viewModel.selectedDiagnosticCommandProperty().get());
        assertFalse(viewModel.diagnosticCommandLoadingProperty().get());
        assertFalse(viewModel.diagnosticCommandErrorProperty().get());
    }

    @Test
    void connectedSessionLoadsJmcAgentStatusAndPresets() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmcAgentService agent = new FakeJmcAgentService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, agent);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmcAgentPreset preset = new JmcAgentPreset("blank", "Blank", "Clear probes",
                "<jfragent><events/></jfragent>");
        JmcAgentStatus status = new JmcAgentStatus(true, "JMC Agent is available.",
                "<jfragent/>", List.of(new JmcAgentTransform("probe", "demo.Service", "run", "()V")));
        agent.setPresets(List.of(preset));
        agent.setStatus(connected.id(), status);

        viewModel.selectedConnectionProperty().set(connected);

        assertTrue(viewModel.jmcAgentAvailableProperty().get());
        assertEquals("JMC Agent is available.", viewModel.jmcAgentStatusMessageProperty().get());
        assertEquals("<jfragent/>", viewModel.jmcAgentConfigurationProperty().get());
        assertEquals(List.of(new JmcAgentTransform("probe", "demo.Service", "run", "()V")),
                viewModel.jmcAgentTransformsProperty());
        assertEquals(List.of(preset), viewModel.jmcAgentPresetsProperty());
        assertEquals(preset, viewModel.selectedJmcAgentPresetProperty().get());
        assertFalse(viewModel.jmcAgentLoadingProperty().get());
        assertFalse(viewModel.jmcAgentErrorProperty().get());
    }

    @Test
    void unavailableJmcAgentKeepsConfigurationActionsDisabledWithReason() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmcAgentService agent = new FakeJmcAgentService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, agent);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        agent.setStatus(connected.id(), JmcAgentStatus.unavailable("JMC Agent MXBean is not registered."));

        viewModel.selectedConnectionProperty().set(connected);

        assertFalse(viewModel.jmcAgentAvailableProperty().get());
        assertEquals("JMC Agent MXBean is not registered.", viewModel.jmcAgentStatusMessageProperty().get());
        assertEquals("", viewModel.jmcAgentConfigurationProperty().get());
        assertTrue(viewModel.jmcAgentTransformsProperty().isEmpty());
        assertFalse(viewModel.jmcAgentLoadingProperty().get());
        assertFalse(viewModel.jmcAgentErrorProperty().get());
    }

    @Test
    void loadingSelectedJmcAgentPresetCopiesXmlIntoEditor() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmcAgentService agent = new FakeJmcAgentService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, agent);
        JmcAgentPreset preset = new JmcAgentPreset("template", "Template", "", "<jfragent/>");
        agent.setPresets(List.of(preset));
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        agent.setStatus(connected.id(), JmcAgentStatus.unavailable("JMC Agent MXBean is not registered."));
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.loadSelectedJmcAgentPreset();

        assertEquals("<jfragent/>", viewModel.jmcAgentConfigurationProperty().get());
    }

    @Test
    void applyingJmcAgentConfigurationRefreshesStatusAndTransforms() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmcAgentService agent = new FakeJmcAgentService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, agent);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        agent.setStatus(connected.id(), new JmcAgentStatus(true, "JMC Agent is available.",
                "<old/>", List.of()));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.jmcAgentConfigurationProperty().set("<new/>");
        agent.setStatus(connected.id(), new JmcAgentStatus(true, "JMC Agent is available.",
                "<new/>", List.of(new JmcAgentTransform("next", "demo.Next", "call", "()V"))));

        viewModel.applyJmcAgentConfiguration();

        assertEquals(connected.id(), agent.lastAppliedConnectionId());
        assertEquals("<new/>", agent.lastAppliedXml());
        assertEquals("<new/>", viewModel.jmcAgentConfigurationProperty().get());
        assertEquals(List.of(new JmcAgentTransform("next", "demo.Next", "call", "()V")),
                viewModel.jmcAgentTransformsProperty());
        assertFalse(viewModel.jmcAgentLoadingProperty().get());
        assertFalse(viewModel.jmcAgentErrorProperty().get());
    }

    @Test
    void executeSelectedDiagnosticCommandStoresOutput() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeDiagnosticCommandService diagnostics = new FakeDiagnosticCommandService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, diagnostics);
        JvmConnection connected = connectedWithDiagnosticCommands(viewModel, jmx);
        DiagnosticCommandInfo threadPrint = new DiagnosticCommandInfo("Thread.print",
                "Thread Print", "Prints threads.", List.of());
        diagnostics.setCommands(connected.id(), List.of(threadPrint));
        diagnostics.setResult(connected.id(), threadPrint.name(),
                new DiagnosticCommandResult(true, "thread dump", ""));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.diagnosticCommandArgumentsProperty().set("-l 100");

        viewModel.executeSelectedDiagnosticCommand();

        assertEquals("thread dump", viewModel.diagnosticCommandOutputProperty().get());
        assertFalse(viewModel.diagnosticCommandErrorProperty().get());
        assertEquals(List.of("-l", "100"), diagnostics.lastRequest().arguments());
    }

    @Test
    void failedDiagnosticCommandSetsErrorAndOutput() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeDiagnosticCommandService diagnostics = new FakeDiagnosticCommandService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, diagnostics);
        JvmConnection connected = connectedWithDiagnosticCommands(viewModel, jmx);
        DiagnosticCommandInfo heapDump = new DiagnosticCommandInfo("GC.heap_dump",
                "GC Heap Dump", "Dumps the heap.", List.of());
        diagnostics.setCommands(connected.id(), List.of(heapDump));
        diagnostics.setResult(connected.id(), heapDump.name(),
                new DiagnosticCommandResult(false, "partial output", "permission denied"));
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.executeSelectedDiagnosticCommand();

        assertTrue(viewModel.diagnosticCommandErrorProperty().get());
        assertEquals("permission denied", viewModel.diagnosticCommandErrorMessageProperty().get());
        assertEquals("permission denied", viewModel.diagnosticCommandOutputProperty().get());
    }

    @Test
    void connectedSessionLoadsMetricDefinitionsForTriggers() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, metrics);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        LiveMetricDefinition heap = new LiveMetricDefinition(
                LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0);
        LiveMetricDefinition threads = new LiveMetricDefinition(
                LiveMetricKind.THREAD_COUNT, "Threads", "threads", 250.0);
        metrics.setDefinitions(connected.id(), List.of(heap, threads));

        viewModel.selectedConnectionProperty().set(connected);

        assertEquals(List.of(heap, threads), viewModel.liveMetricDefinitionsProperty());
        assertEquals(heap, viewModel.selectedTriggerMetricProperty().get());
        assertFalse(viewModel.triggerLoadingProperty().get());
        assertFalse(viewModel.triggerErrorProperty().get());
    }

    @Test
    void connectedSessionLoadsOverviewMetricsForChartAndTable() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, metrics);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        metrics.setDefinitions(connected.id(), List.of(
                new LiveMetricDefinition(LiveMetricKind.PROCESS_CPU_LOAD_PERCENT, "Process CPU", "%", 80.0),
                new LiveMetricDefinition(LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0),
                new LiveMetricDefinition(LiveMetricKind.THREAD_COUNT, "Thread Count", "threads", 250.0)));
        metrics.setSnapshot(connected.id(), List.of(
                new LiveMetricSnapshot(LiveMetricKind.PROCESS_CPU_LOAD_PERCENT, 52.5, "%", Instant.EPOCH),
                new LiveMetricSnapshot(LiveMetricKind.HEAP_USED_PERCENT, 64.0, "%", Instant.EPOCH),
                new LiveMetricSnapshot(LiveMetricKind.THREAD_COUNT, 18.0, "threads", Instant.EPOCH)));

        viewModel.selectedConnectionProperty().set(connected);

        assertEquals(List.of("Processor", "Memory", "Dashboard"),
                viewModel.overviewMetricsProperty().stream().map(LiveJvmOverviewMetric::group).toList());
        assertEquals(List.of("52.5%", "64.0%", "18"),
                viewModel.overviewMetricsProperty().stream().map(LiveJvmOverviewMetric::displayValue).toList());
        assertFalse(viewModel.overviewLoadingProperty().get());
        assertFalse(viewModel.overviewErrorProperty().get());
        assertFalse(viewModel.overviewPersistenceProperty().get().configured());
    }

    @Test
    void evaluateTriggersAppendsNotifyEventWhenThresholdMatches() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, metrics);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        LiveMetricDefinition heap = new LiveMetricDefinition(
                LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0);
        metrics.setDefinitions(connected.id(), List.of(heap));
        metrics.setSnapshot(connected.id(), List.of(new LiveMetricSnapshot(
                LiveMetricKind.HEAP_USED_PERCENT, 91.0, "%", Instant.EPOCH)));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.triggerNameProperty().set("Heap high");
        viewModel.triggerThresholdProperty().set("80");
        viewModel.addTriggerRule();

        viewModel.evaluateTriggersNow();

        assertEquals(1, viewModel.triggerEventsProperty().size());
        TriggerEvent event = viewModel.triggerEventsProperty().getFirst();
        assertEquals("Heap high", event.ruleName());
        assertTrue(event.message().contains("91.0"));
    }

    @Test
    void evaluateTriggersUsesRuleSnapshotCapturedWhenQueued() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        QueuedJvmBrowserExecutor executor = new QueuedJvmBrowserExecutor();
        JvmBrowserViewModel viewModel = new JvmBrowserViewModel(new FakeJvmDiscoveryService(), jmx, null, null,
                null, metrics, executor, Runnable::run, path -> { });
        JvmConnection connected = JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        LiveMetricDefinition heap = new LiveMetricDefinition(
                LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0);
        metrics.setSnapshot(connected.id(), List.of(new LiveMetricSnapshot(
                LiveMetricKind.HEAP_USED_PERCENT, 91.0, "%", Instant.EPOCH)));
        viewModel.selectedSessionProperty().set(sessionSnapshot(connected));
        viewModel.selectedTriggerMetricProperty().set(heap);
        viewModel.triggerNameProperty().set("Heap high");
        viewModel.triggerThresholdProperty().set("80");
        viewModel.addTriggerRule();

        viewModel.evaluateTriggersNow();
        viewModel.triggerRulesProperty().clear();
        executor.runNext();

        assertEquals(1, viewModel.triggerEventsProperty().size());
        TriggerEvent event = viewModel.triggerEventsProperty().getFirst();
        assertEquals("Heap high", event.ruleName());
        assertEquals(91.0, event.value());
    }

    @Test
    void diagnosticCommandTriggerExecutesCommandWhenThresholdMatches() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        FakeDiagnosticCommandService diagnostics = new FakeDiagnosticCommandService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, diagnostics, metrics);
        JvmConnection connected = connectedWithDiagnosticCommands(viewModel, jmx);
        LiveMetricDefinition threads = new LiveMetricDefinition(
                LiveMetricKind.THREAD_COUNT, "Threads", "threads", 250.0);
        DiagnosticCommandInfo threadPrint = new DiagnosticCommandInfo(
                "threadPrint", "Thread Print", "Prints threads.", List.of());
        metrics.setDefinitions(connected.id(), List.of(threads));
        metrics.setSnapshot(connected.id(), List.of(new LiveMetricSnapshot(
                LiveMetricKind.THREAD_COUNT, 300.0, "threads", Instant.EPOCH)));
        diagnostics.setCommands(connected.id(), List.of(threadPrint));
        diagnostics.setResult(connected.id(), threadPrint.name(),
                new DiagnosticCommandResult(true, "dump", ""));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.triggerNameProperty().set("Thread count high");
        viewModel.triggerThresholdProperty().set("250");
        viewModel.selectedTriggerActionTypeProperty().set(TriggerActionType.DIAGNOSTIC_COMMAND);
        viewModel.selectedTriggerCommandProperty().set(threadPrint);
        viewModel.addTriggerRule();

        viewModel.evaluateTriggersNow();

        assertEquals("threadPrint", diagnostics.lastRequest().commandName());
        assertEquals(1, viewModel.triggerEventsProperty().size());
        assertTrue(viewModel.triggerEventsProperty().getFirst().message().contains("dump"));
    }

    @Test
    void diagnosticCommandTriggerRequiresSelectedCommand() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        FakeDiagnosticCommandService diagnostics = new FakeDiagnosticCommandService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, diagnostics, metrics);
        LiveMetricDefinition threads = new LiveMetricDefinition(
                LiveMetricKind.THREAD_COUNT, "Threads", "threads", 250.0);
        viewModel.selectedTriggerMetricProperty().set(threads);
        viewModel.triggerThresholdProperty().set("250");
        viewModel.selectedTriggerActionTypeProperty().set(TriggerActionType.DIAGNOSTIC_COMMAND);

        viewModel.addTriggerRule();

        assertTrue(viewModel.triggerErrorProperty().get());
        assertEquals("Select a Diagnostic Command for this trigger.", viewModel.triggerErrorMessageProperty().get());
        assertTrue(viewModel.triggerRulesProperty().isEmpty());
    }

    @Test
    void clearingSelectedSessionClearsSelectedTriggerEditorState() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeDiagnosticCommandService diagnostics = new FakeDiagnosticCommandService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, diagnostics, metrics);
        JvmConnection connected = connectedWithDiagnosticCommands(viewModel, jmx);
        LiveMetricDefinition threads = new LiveMetricDefinition(
                LiveMetricKind.THREAD_COUNT, "Threads", "threads", 250.0);
        DiagnosticCommandInfo threadPrint = new DiagnosticCommandInfo(
                "threadPrint", "Thread Print", "Prints threads.", List.of());
        metrics.setDefinitions(connected.id(), List.of(threads));
        diagnostics.setCommands(connected.id(), List.of(threadPrint));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedTriggerCommandProperty().set(threadPrint);
        viewModel.selectedTriggerOperatorProperty().set(TriggerOperator.LESS_THAN);
        viewModel.selectedTriggerActionTypeProperty().set(TriggerActionType.DIAGNOSTIC_COMMAND);

        viewModel.selectedConnectionProperty().set(null);

        assertNull(viewModel.selectedSessionProperty().get());
        assertNull(viewModel.selectedTriggerCommandProperty().get());
        assertEquals(TriggerOperator.GREATER_THAN_OR_EQUAL,
                viewModel.selectedTriggerOperatorProperty().get());
        assertEquals(TriggerActionType.NOTIFY, viewModel.selectedTriggerActionTypeProperty().get());
    }

    @Test
    void connectedSessionLoadsPersistedMonitoringState() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", connected.id(), "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 3, true, true);
        JmxSubscriptionSample sample = new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH, 50, "50", "%", true);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", connected.id(), "demo:type=Notifier", "Notifier", 2, true, true);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo", "source", 1, "message", "");
        repository.saveAttributeSubscription(subscription);
        repository.appendSample(sample);
        repository.saveNotificationSubscription(notificationSubscription);
        repository.appendNotificationEvent(event);

        viewModel.selectedConnectionProperty().set(connected);

        assertTrue(viewModel.jmxMonitoringAvailableProperty().get());
        assertEquals(List.of(subscription), viewModel.jmxAttributeSubscriptionsProperty());
        assertEquals(subscription, viewModel.selectedJmxAttributeSubscriptionProperty().get());
        assertEquals(List.of(sample), viewModel.jmxSubscriptionSamplesProperty());
        assertEquals(List.of(notificationSubscription), viewModel.jmxNotificationSubscriptionsProperty());
        assertEquals(notificationSubscription, viewModel.selectedJmxNotificationSubscriptionProperty().get());
        assertEquals(List.of(event), viewModel.jmxNotificationEventsProperty());
    }

    @Test
    void addSelectedMBeanAttributeSubscriptionCreatesBoundedSubscription() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanAttributeInfo uptime = new MBeanAttributeInfo("Uptime", "long", true, false, "1000", "");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        mbeans.setAttributes(connected.id(), runtime.objectName(), List.of(uptime));
        mbeans.setOperations(connected.id(), runtime.objectName(), List.of());
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(runtime);

        viewModel.addMBeanAttributeSubscription(uptime, Duration.ofSeconds(2), 3, true);

        assertEquals(1, viewModel.jmxAttributeSubscriptionsProperty().size());
        JmxAttributeSubscription subscription = viewModel.jmxAttributeSubscriptionsProperty().getFirst();
        assertEquals(connected.id(), subscription.connectionId());
        assertEquals(runtime.objectName(), subscription.objectName());
        assertEquals("Uptime", subscription.attributeName());
        assertEquals(Duration.ofSeconds(2), subscription.samplingInterval());
        assertEquals(3, subscription.maxSamples());
        assertEquals(List.of(subscription), repository.findAttributeSubscriptions(connected.id()));
    }

    @Test
    void addMBeanNotificationSubscriptionCreatesPersistedSubscription() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode notifier = MBeanNode.objectName("demo:type=Notifier", "Notifier");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("demo", List.of(notifier))));
        mbeans.setAttributes(connected.id(), notifier.objectName(), List.of());
        mbeans.setOperations(connected.id(), notifier.objectName(), List.of());
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(notifier);

        viewModel.addMBeanNotificationSubscription(notifier, 100, true);

        assertEquals(1, viewModel.jmxNotificationSubscriptionsProperty().size());
        JmxNotificationSubscription subscription = viewModel.jmxNotificationSubscriptionsProperty().getFirst();
        assertEquals(connected.id(), subscription.connectionId());
        assertEquals("demo:type=Notifier", subscription.objectName());
        assertEquals("Notifier", subscription.label());
        assertEquals(100, subscription.maxEvents());
        assertTrue(subscription.enabled());
        assertTrue(subscription.persisted());
        assertEquals(subscription, viewModel.selectedJmxNotificationSubscriptionProperty().get());
        assertEquals(List.of(subscription), repository.findNotificationSubscriptions(connected.id()));
    }

    @Test
    void addMBeanNotificationSubscriptionRejectsDomainNode() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.addMBeanNotificationSubscription(MBeanNode.domain("demo", List.of()), 100, true);

        assertTrue(viewModel.jmxMonitoringErrorProperty().get());
        assertEquals("Select an MBean object to subscribe to notifications.",
                viewModel.jmxMonitoringErrorMessageProperty().get());
        assertTrue(viewModel.jmxNotificationSubscriptionsProperty().isEmpty());
    }

    @Test
    void sampleSelectedSubscriptionAppendsSampleAndPersistsIt() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", connected.id(), "java.lang:type=Threading", "ThreadCount",
                "Thread Count", "threads", Duration.ofSeconds(1), 3, true, true);
        JmxSubscriptionSample sample = new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH, 301, "301", "threads", true);
        monitoring.setSample(connected.id(), "sub-1", sample);
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.jmxAttributeSubscriptionsProperty().add(subscription);
        viewModel.selectedJmxAttributeSubscriptionProperty().set(subscription);

        viewModel.sampleSelectedJmxSubscriptionNow();

        assertEquals(List.of(sample), viewModel.jmxSubscriptionSamplesProperty());
        assertEquals(List.of(sample), repository.findSamples("sub-1"));
    }

    @Test
    void notificationEventsAreBoundedAndPersisted() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", connected.id(), "demo:type=Notifier", "Notifier", 1, true, true);
        JmxNotificationEvent first = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "one", "demo", 1, "one", "");
        JmxNotificationEvent second = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH.plusSeconds(1), "two", "demo", 2, "two", "");
        monitoring.setNotificationEvents(connected.id(), "notif-1", List.of(first, second));
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.startJmxNotifications(subscription);

        assertEquals(List.of(second), viewModel.jmxNotificationEventsProperty());
        assertEquals(List.of(second), repository.findNotificationEvents("notif-1"));
    }

    @Test
    void startSelectedJmxNotificationsAppendsInitialEvents() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", connected.id(), "demo:type=Notifier", "Notifier", 2, true, true);
        JmxNotificationEvent first = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo.first", "source", 1, "first", "");
        JmxNotificationEvent second = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH.plusSeconds(1), "demo.second", "source", 2, "second", "");
        monitoring.setNotificationEvents(connected.id(), subscription.id(), List.of(first, second));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedJmxNotificationSubscriptionProperty().set(subscription);

        viewModel.startSelectedJmxNotifications();

        assertEquals(List.of(first, second), viewModel.jmxNotificationEventsProperty());
        assertEquals(List.of(subscription), repository.findNotificationSubscriptions(connected.id()));
        assertEquals(List.of(first, second), repository.findNotificationEvents(subscription.id()));
        assertFalse(viewModel.jmxMonitoringLoadingProperty().get());
        assertFalse(viewModel.jmxMonitoringErrorProperty().get());
    }

    @Test
    void stopSelectedJmxNotificationsStopsServiceAndKeepsEvents() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", connected.id(), "demo:type=Notifier", "Notifier", 2, true, false);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo", "source", 1, "message", "");
        repository.saveNotificationSubscription(subscription);
        repository.appendNotificationEvent(event);
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedJmxNotificationSubscriptionProperty().set(subscription);

        viewModel.stopSelectedJmxNotifications();

        assertEquals(List.of(subscription.id()), monitoring.stoppedNotificationIds());
        assertEquals(List.of(event), viewModel.jmxNotificationEventsProperty());
        assertFalse(viewModel.jmxMonitoringLoadingProperty().get());
        assertFalse(viewModel.jmxMonitoringErrorProperty().get());
    }

    @Test
    void clearingSelectedSessionClearsMonitoringState() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", connected.id(), "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 3, true, true);
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.jmxAttributeSubscriptionsProperty().add(subscription);
        viewModel.selectedJmxAttributeSubscriptionProperty().set(subscription);

        viewModel.selectedConnectionProperty().set(null);

        assertFalse(viewModel.jmxMonitoringAvailableProperty().get());
        assertTrue(viewModel.jmxAttributeSubscriptionsProperty().isEmpty());
        assertTrue(viewModel.jmxSubscriptionSamplesProperty().isEmpty());
        assertNull(viewModel.selectedJmxAttributeSubscriptionProperty().get());
    }

    @Test
    void clearingSelectedSessionClearsOverviewState() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeLiveMetricService metrics = new FakeLiveMetricService();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, metrics);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        metrics.setDefinitions(connected.id(), List.of(
                new LiveMetricDefinition(LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0)));
        metrics.setSnapshot(connected.id(), List.of(
                new LiveMetricSnapshot(LiveMetricKind.HEAP_USED_PERCENT, 64.0, "%", Instant.EPOCH)));
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.selectedConnectionProperty().set(null);

        assertTrue(viewModel.overviewMetricsProperty().isEmpty());
        assertFalse(viewModel.overviewPersistenceProperty().get().configured());
        assertFalse(viewModel.overviewLoadingProperty().get());
        assertFalse(viewModel.overviewErrorProperty().get());
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx) {
        return new JvmBrowserViewModel(discovery, jmx, new DirectJvmBrowserExecutor(), Runnable::run);
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeFlightRecordingService recordings) {
        return new JvmBrowserViewModel(discovery, jmx, recordings, new DirectJvmBrowserExecutor(), Runnable::run,
                path -> { });
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeMBeanBrowserService mbeans) {
        return new JvmBrowserViewModel(discovery, jmx, mbeans, new DirectJvmBrowserExecutor(), Runnable::run);
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeMBeanBrowserService mbeans, FakeJmxMonitoringService monitoring,
            FakeJmxMonitoringRepository repository) {
        return new JvmBrowserViewModel(discovery, jmx, null, mbeans, null, null, null,
                monitoring, repository, null, null, new DirectJvmBrowserExecutor(), Runnable::run, path -> { });
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeDiagnosticCommandService diagnostics) {
        return new JvmBrowserViewModel(discovery, jmx, null, null, diagnostics, null,
                new DirectJvmBrowserExecutor(), Runnable::run, path -> { });
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeLiveMetricService metrics) {
        return new JvmBrowserViewModel(discovery, jmx, null, null, null, metrics,
                new DirectJvmBrowserExecutor(), Runnable::run, path -> { });
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeJmcAgentService agent) {
        return new JvmBrowserViewModel(discovery, jmx, null, null, null, null, agent,
                new DirectJvmBrowserExecutor(), Runnable::run, path -> { });
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeDiagnosticCommandService diagnostics, FakeLiveMetricService metrics) {
        return new JvmBrowserViewModel(discovery, jmx, null, null, diagnostics, metrics,
                new DirectJvmBrowserExecutor(), Runnable::run, path -> { });
    }

    private static JvmBrowserViewModel viewModel(FakeJvmDiscoveryService discovery, FakeJmxConnectionService jmx,
            FakeSavedJvmTargetRepository savedTargets, FakeJdpDiscoveryService jdp) {
        return new JvmBrowserViewModel(discovery, jmx, null, null, null, null, savedTargets, jdp,
                new DirectJvmBrowserExecutor(), Runnable::run, path -> { });
    }

    private static JvmConnection localConnection(String id, String name) {
        return JvmConnection.local(id, name, "26.0.1", true);
    }

    private static JvmConnection connectedWithFlightRecorder(JvmBrowserViewModel viewModel, FakeJmxConnectionService jmx,
            FakeFlightRecordingService recordings) {
        JvmConnection connected = JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        jmx.setSessionSnapshot("42", flightRecorderSessionSnapshot(connected));
        recordings.setAvailable("42", true);
        recordings.addRecording("42", new FlightRecordingInfo(100, "Existing", FlightRecordingState.RUNNING,
                1_000, 4096));
        viewModel.connectionsProperty().add(connected);
        return connected;
    }

    private static JvmConnection connectedWithMBeans(JvmBrowserViewModel viewModel, FakeJmxConnectionService jmx) {
        return connectedWithMBeans(viewModel, jmx, "42");
    }

    private static JvmConnection connectedWithMBeans(JvmBrowserViewModel viewModel, FakeJmxConnectionService jmx,
            String id) {
        JvmConnection connected = JvmConnection.local(id, "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        jmx.setSessionSnapshot(id, sessionSnapshot(connected));
        viewModel.connectionsProperty().add(connected);
        return connected;
    }

    private static JvmConnection connectedWithDiagnosticCommands(JvmBrowserViewModel viewModel,
            FakeJmxConnectionService jmx) {
        JvmConnection connected = JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        jmx.setSessionSnapshot("42", diagnosticCommandsSessionSnapshot(connected));
        viewModel.connectionsProperty().add(connected);
        return connected;
    }

    private static JvmSessionSnapshot sessionSnapshot(JvmConnection connection) {
        return new JvmSessionSnapshot(connection,
                new JvmRuntimeSnapshot("OpenJDK 64-Bit Server VM", "Eclipse Adoptium",
                        "26.0.1", "26", Instant.EPOCH, 1000),
                List.of(new JvmCapabilitySnapshot(JvmCapability.MBEAN_SERVER,
                        JvmCapabilityStatus.AVAILABLE, "Available")));
    }

    private static JvmSessionSnapshot flightRecorderSessionSnapshot(JvmConnection connection) {
        return new JvmSessionSnapshot(connection,
                new JvmRuntimeSnapshot("OpenJDK 64-Bit Server VM", "Eclipse Adoptium",
                        "26.0.1", "26", Instant.EPOCH, 1000),
                List.of(new JvmCapabilitySnapshot(JvmCapability.MBEAN_SERVER,
                                JvmCapabilityStatus.AVAILABLE, "Available"),
                        new JvmCapabilitySnapshot(JvmCapability.FLIGHT_RECORDER,
                                JvmCapabilityStatus.AVAILABLE, "Available")));
    }

    private static JvmSessionSnapshot diagnosticCommandsSessionSnapshot(JvmConnection connection) {
        return new JvmSessionSnapshot(connection,
                new JvmRuntimeSnapshot("OpenJDK 64-Bit Server VM", "Eclipse Adoptium",
                        "26.0.1", "26", Instant.EPOCH, 1000),
                List.of(new JvmCapabilitySnapshot(JvmCapability.MBEAN_SERVER,
                                JvmCapabilityStatus.AVAILABLE, "Available"),
                        new JvmCapabilitySnapshot(JvmCapability.DIAGNOSTIC_COMMANDS,
                                JvmCapabilityStatus.AVAILABLE, "Available")));
    }

    private static final class QueuedJvmBrowserExecutor implements JvmBrowserExecutor {
        private final Queue<Runnable> queue = new ArrayDeque<>();

        @Override
        public void execute(Runnable runnable) {
            queue.add(runnable);
        }

        private void runNext() {
            queue.remove().run();
        }

        private void runLast() {
            List<Runnable> queued = new ArrayList<>(queue);
            queue.clear();
            queued.getLast().run();
            queue.addAll(queued.subList(0, queued.size() - 1));
        }

        private boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    private static RecordingAppender attachRecordingAppender() {
        Logger logger = (Logger) LogManager.getLogger(JvmBrowserViewModel.class);
        RecordingAppender appender = new RecordingAppender(logger.getLevel(), logger.isAdditive());
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        return appender;
    }

    private static void detachRecordingAppender(RecordingAppender appender) {
        Logger logger = (Logger) LogManager.getLogger(JvmBrowserViewModel.class);
        logger.removeAppender(appender);
        logger.setLevel(appender.previousLevel);
        logger.setAdditive(appender.previousAdditive);
        appender.stop();
    }

    private static final class RecordingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();
        private final Level previousLevel;
        private final boolean previousAdditive;

        private RecordingAppender(Level previousLevel, boolean previousAdditive) {
            super("recording", (Filter) null, (Layout<?>) null, false, Property.EMPTY_ARRAY);
            this.previousLevel = previousLevel;
            this.previousAdditive = previousAdditive;
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }

    private static final class CapturingMBeanBrowserService extends FakeMBeanBrowserService {
        private MBeanOperationRequest lastRequest;

        @Override
        public MBeanOperationResult invoke(MBeanOperationRequest request) {
            lastRequest = request;
            return super.invoke(request);
        }
    }

    private static final class CapturingRemoteJmxConnectionService extends FakeJmxConnectionService {
        private final List<String> remoteConnectionUrls = new ArrayList<>();
        private JvmConnection connectedToReturn;

        @Override
        public JvmConnection connect(String connectionUrl) {
            remoteConnectionUrls.add(connectionUrl);
            if (connectedToReturn != null) {
                return connectedToReturn;
            }
            return super.connect(connectionUrl);
        }
    }
}
