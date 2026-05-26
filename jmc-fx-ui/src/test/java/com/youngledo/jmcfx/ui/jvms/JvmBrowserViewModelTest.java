package com.youngledo.jmcfx.ui.jvms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

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
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.testsupport.FakeFlightRecordingService;
import com.youngledo.jmcfx.testsupport.FakeJmxConnectionService;
import com.youngledo.jmcfx.testsupport.FakeJvmDiscoveryService;
import com.youngledo.jmcfx.testsupport.FakeMBeanBrowserService;

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
        JvmConnection connected = JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        jmx.setSessionSnapshot("42", sessionSnapshot(connected));
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
    }

    private static final class CapturingMBeanBrowserService extends FakeMBeanBrowserService {
        private MBeanOperationRequest lastRequest;

        @Override
        public MBeanOperationResult invoke(MBeanOperationRequest request) {
            lastRequest = request;
            return super.invoke(request);
        }
    }
}
