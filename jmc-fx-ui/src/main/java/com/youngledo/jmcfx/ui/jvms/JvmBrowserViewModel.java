package com.youngledo.jmcfx.ui.jvms;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingState;
import com.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import com.youngledo.jmcfx.domain.service.MBeanBrowserService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JvmBrowserViewModel implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(JvmBrowserViewModel.class);
    private static final DateTimeFormatter RECORDING_NAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JvmDiscoveryService discoveryService;
    private final JmxConnectionService connectionService;
    private final FlightRecordingService flightRecordingService;
    private final MBeanBrowserService mBeanBrowserService;
    private final JvmBrowserExecutor executor;
    private final Consumer<Runnable> fxRunner;
    private final Consumer<Path> savedRecordingHandler;
    private final ObservableList<JvmConnection> connections = FXCollections.observableArrayList();
    private final ObservableList<FlightRecordingInfo> flightRecordings = FXCollections.observableArrayList();
    private final ObservableList<MBeanNode> mbeanTree = FXCollections.observableArrayList();
    private final ObservableList<MBeanAttributeInfo> mbeanAttributes = FXCollections.observableArrayList();
    private final ObservableList<MBeanOperationInfo> mbeanOperations = FXCollections.observableArrayList();
    private final ObjectProperty<JvmConnection> selectedConnection = new SimpleObjectProperty<>();
    private final ObjectProperty<FlightRecordingInfo> selectedFlightRecording = new SimpleObjectProperty<>();
    private final ObjectProperty<MBeanNode> selectedMBean = new SimpleObjectProperty<>();
    private final ObjectProperty<MBeanOperationInfo> selectedMBeanOperation = new SimpleObjectProperty<>();
    private final StringProperty manualConnectionUrl = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty mbeanErrorMessage = new SimpleStringProperty("");
    private final StringProperty mbeanOperationArguments = new SimpleStringProperty("");
    private final StringProperty mbeanOperationResult = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty error = new SimpleBooleanProperty(false);
    private final BooleanProperty refreshCompleted = new SimpleBooleanProperty(false);
    private final ObjectProperty<JvmSessionSnapshot> selectedSession = new SimpleObjectProperty<>();
    private final BooleanProperty sessionLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty sessionError = new SimpleBooleanProperty(false);
    private final StringProperty sessionErrorMessage = new SimpleStringProperty("");
    private final BooleanProperty recordingControlAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty recordingLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty recordingError = new SimpleBooleanProperty(false);
    private final StringProperty recordingErrorMessage = new SimpleStringProperty("");
    private final StringProperty recordingStatusMessage = new SimpleStringProperty("");
    private final BooleanProperty mbeanBrowserAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty mbeanLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty mbeanError = new SimpleBooleanProperty(false);
    private final Map<Long, JvmConnection> sessionStartedRecordings = new HashMap<>();
    private int pendingWorkCount;
    private long sessionLoadGeneration;
    private long mbeanRequestGeneration;

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService) {
        this(discoveryService, connectionService, null, new VirtualThreadJvmBrowserExecutor(),
                javafx.application.Platform::runLater, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner) {
        this(discoveryService, connectionService, null, executor, fxRunner, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            MBeanBrowserService mBeanBrowserService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner) {
        this(discoveryService, connectionService, null, mBeanBrowserService, executor, fxRunner, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner,
            Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, null, executor, fxRunner,
                savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner, Consumer<Path> savedRecordingHandler) {
        this.discoveryService = Objects.requireNonNull(discoveryService, "discoveryService");
        this.connectionService = Objects.requireNonNull(connectionService, "connectionService");
        this.flightRecordingService = flightRecordingService;
        this.mBeanBrowserService = mBeanBrowserService;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.fxRunner = Objects.requireNonNull(fxRunner, "fxRunner");
        this.savedRecordingHandler = Objects.requireNonNull(savedRecordingHandler, "savedRecordingHandler");
        this.selectedConnection.addListener((observable, oldValue, newValue) -> loadSessionForSelection(newValue));
        this.selectedMBean.addListener((observable, oldValue, newValue) -> loadSelectedMBeanDetails(newValue));
        this.selectedMBeanOperation.addListener((observable, oldValue, newValue) -> clearMBeanOperationResult());
    }

    public ObservableList<JvmConnection> connectionsProperty() {
        return connections;
    }

    public ObjectProperty<JvmConnection> selectedConnectionProperty() {
        return selectedConnection;
    }

    public ObservableList<FlightRecordingInfo> flightRecordingsProperty() {
        return flightRecordings;
    }

    public ObjectProperty<FlightRecordingInfo> selectedFlightRecordingProperty() {
        return selectedFlightRecording;
    }

    public StringProperty manualConnectionUrlProperty() {
        return manualConnectionUrl;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public BooleanProperty errorProperty() {
        return error;
    }

    public BooleanProperty refreshCompletedProperty() {
        return refreshCompleted;
    }

    public ObjectProperty<JvmSessionSnapshot> selectedSessionProperty() {
        return selectedSession;
    }

    public BooleanProperty sessionLoadingProperty() {
        return sessionLoading;
    }

    public BooleanProperty sessionErrorProperty() {
        return sessionError;
    }

    public StringProperty sessionErrorMessageProperty() {
        return sessionErrorMessage;
    }

    public BooleanProperty recordingControlAvailableProperty() {
        return recordingControlAvailable;
    }

    public BooleanProperty recordingLoadingProperty() {
        return recordingLoading;
    }

    public BooleanProperty recordingErrorProperty() {
        return recordingError;
    }

    public StringProperty recordingErrorMessageProperty() {
        return recordingErrorMessage;
    }

    public StringProperty recordingStatusMessageProperty() {
        return recordingStatusMessage;
    }

    public ObservableList<MBeanNode> mbeanTreeProperty() {
        return mbeanTree;
    }

    public ObservableList<MBeanAttributeInfo> mbeanAttributesProperty() {
        return mbeanAttributes;
    }

    public ObservableList<MBeanOperationInfo> mbeanOperationsProperty() {
        return mbeanOperations;
    }

    public ObjectProperty<MBeanNode> selectedMBeanProperty() {
        return selectedMBean;
    }

    public ObjectProperty<MBeanOperationInfo> selectedMBeanOperationProperty() {
        return selectedMBeanOperation;
    }

    public BooleanProperty mbeanBrowserAvailableProperty() {
        return mbeanBrowserAvailable;
    }

    public BooleanProperty mbeanLoadingProperty() {
        return mbeanLoading;
    }

    public BooleanProperty mbeanErrorProperty() {
        return mbeanError;
    }

    public StringProperty mbeanErrorMessageProperty() {
        return mbeanErrorMessage;
    }

    public StringProperty mbeanOperationArgumentsProperty() {
        return mbeanOperationArguments;
    }

    public StringProperty mbeanOperationResultProperty() {
        return mbeanOperationResult;
    }

    public FlightRecordingInfo selectedFlightRecording() {
        return selectedFlightRecording.get();
    }

    public void refresh() {
        beginWork();
        executor.execute(() -> {
            try {
                List<JvmConnection> discovered = discoveryService.discoverLocalJvms();
                runOnFx(() -> {
                    mergeDiscovered(discovered);
                    refreshCompleted.set(true);
                    statusMessage.set("");
                    clearError();
                    finishWork();
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    public void connectSelectedOrManual() {
        String url = manualConnectionUrl.get() == null ? "" : manualConnectionUrl.get().trim();
        if (!url.isBlank()) {
            connectManual(url);
            return;
        }
        connectSelected();
    }

    public void connectSelected() {
        JvmConnection selected = selectedConnection.get();
        if (!canConnectJvm(selected)) {
            error.set(true);
            errorMessage.set("Enter a JMX service URL or select an attachable JVM.");
            return;
        }
        connectLocal(selected);
    }

    public void connectManual() {
        connectSelectedOrManual();
    }

    public void disconnectSelected() {
        JvmConnection selected = selectedConnection.get();
        if (selected == null || !selected.connected()) {
            error.set(true);
            errorMessage.set("Select a connected JVM to disconnect.");
            return;
        }
        beginWork();
        executor.execute(() -> {
            try {
                connectionService.disconnect(selected);
                JvmConnection disconnected = selected.asDisconnected("Disconnected");
                runOnFx(() -> {
                    replaceOrAdd(disconnected, "Disconnected.");
                    selectedSession.set(null);
                    clearRecordingControl();
                    clearSessionError();
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    public void startFlightRecording() {
        JvmConnection selected = selectedConnection.get();
        if (!canUseRecordingControl(selected)) {
            recordingError.set(true);
            recordingErrorMessage.set("Select a connected JVM with Flight Recorder available.");
            return;
        }
        recordingLoading.set(true);
        clearRecordingError();
        executor.execute(() -> {
            try {
                FlightRecordingStartRequest request = new FlightRecordingStartRequest(selected,
                        recordingName(selected), FlightRecordingTemplate.profile());
                FlightRecordingInfo started = flightRecordingService.startRecording(request);
                sessionStartedRecordings.put(started.id(), selected);
                List<FlightRecordingInfo> updated = flightRecordingService.recordings(selected);
                runOnFx(() -> {
                    flightRecordings.setAll(updated);
                    selectedFlightRecording.set(updated.isEmpty() ? null : updated.getLast());
                    recordingStatusMessage.set("");
                    recordingLoading.set(false);
                });
            } catch (RuntimeException exception) {
                failRecording(exception);
            }
        });
    }

    public void stopAndSaveSelectedFlightRecording(Path destinationFile) {
        JvmConnection selectedConnection = this.selectedConnection.get();
        FlightRecordingInfo selectedRecording = selectedFlightRecording.get();
        if (!canUseRecordingControl(selectedConnection) || selectedRecording == null) {
            recordingError.set(true);
            recordingErrorMessage.set("Select a running Flight Recording to save.");
            return;
        }
        recordingLoading.set(true);
        clearRecordingError();
        executor.execute(() -> {
            try {
                Path saved = flightRecordingService.stopAndSaveRecording(new FlightRecordingStopRequest(
                        selectedConnection, selectedRecording.id(), destinationFile));
                sessionStartedRecordings.remove(selectedRecording.id());
                List<FlightRecordingInfo> updated = flightRecordingService.recordings(selectedConnection);
                runOnFx(() -> {
                    flightRecordings.setAll(updated);
                    selectedFlightRecording.set(updated.isEmpty() ? null : updated.getFirst());
                    recordingStatusMessage.set("");
                    recordingLoading.set(false);
                    savedRecordingHandler.accept(saved);
                });
            } catch (RuntimeException exception) {
                failRecording(exception);
            }
        });
    }

    public void refreshSelectedMBeanAttributes() {
        loadSelectedMBeanDetails(selectedMBean.get());
    }

    public void invokeSelectedMBeanOperation() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        MBeanNode node = selectedMBean.get();
        MBeanOperationInfo operation = selectedMBeanOperation.get();
        if (!canUseMBeanBrowser(snapshot) || node == null || node.domain() || operation == null) {
            mbeanError.set(true);
            mbeanErrorMessage.set("Select an MBean operation to invoke.");
            return;
        }

        List<String> parameterTypes = operation.parameters().stream()
                .map(MBeanOperationParameter::type)
                .toList();
        List<String> arguments = parseMBeanArguments(mbeanOperationArguments.get());
        MBeanOperationRequest request = new MBeanOperationRequest(snapshot.connection(), node.objectName(),
                operation.name(), parameterTypes, arguments);
        long generation = nextMBeanRequestGeneration();
        mbeanLoading.set(true);
        clearMBeanError();
        executor.execute(() -> {
            try {
                MBeanOperationResult result = mBeanBrowserService.invoke(request);
                runOnFx(() -> {
                    if (!isCurrentMBeanRequest(generation, snapshot, node, operation)) {
                        return;
                    }
                    if (result.success()) {
                        mbeanOperationResult.set(result.value());
                        clearMBeanError();
                    } else {
                        String message = displayMBeanResult(result);
                        mbeanOperationResult.set(message);
                        mbeanError.set(true);
                        mbeanErrorMessage.set(message);
                    }
                    mbeanLoading.set(false);
                });
            } catch (RuntimeException exception) {
                failMBean(generation, snapshot, node, operation, exception);
            }
        });
    }

    @Override
    public void close() {
        discardSessionStartedRecordings();
        executor.close();
    }

    public static boolean canConnectJvm(JvmConnection selected) {
        return selected != null && !selected.connected() && selected.source() == JvmConnectionSource.LOCAL
                && selected.attachable();
    }

    private void connectManual(String url) {
        beginWork();
        executor.execute(() -> {
            try {
                JvmConnection connected = connectionService.connect(url);
                runOnFx(() -> {
                    connections.add(connected);
                    selectedConnection.set(connected);
                    manualConnectionUrl.set("");
                    statusMessage.set("Connected to " + url + ".");
                    clearError();
                    finishWork();
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    private void connectLocal(JvmConnection selected) {
        beginWork();
        executor.execute(() -> {
            try {
                JvmConnection connected = connectionService.connectLocal(selected);
                runOnFx(() -> replaceOrAdd(connected, "Connected to " + selected.displayName() + "."));
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    private void mergeDiscovered(List<JvmConnection> discovered) {
        JvmConnection selectedBefore = selectedConnection.get();
        String selectedKey = stableKey(selectedBefore);
        Set<String> discoveredKeys = discovered.stream()
                .map(JvmBrowserViewModel::stableKey)
                .collect(Collectors.toSet());
        List<JvmConnection> merged = new ArrayList<>();

        for (JvmConnection existing : connections) {
            if (existing.connected() || existing.source() == JvmConnectionSource.MANUAL) {
                merged.add(existing);
            } else if (existing.source() == JvmConnectionSource.LOCAL
                    && !discoveredKeys.contains(stableKey(existing))) {
                selectedKey = clearSelectedIfRemoved(selectedKey, existing);
            }
        }

        Set<String> protectedKeys = merged.stream().map(JvmBrowserViewModel::stableKey).collect(Collectors.toSet());
        for (JvmConnection next : discovered) {
            String nextKey = stableKey(next);
            if (!protectedKeys.contains(nextKey)) {
                merged.add(next);
                protectedKeys.add(nextKey);
            }
        }

        connections.setAll(merged);
        String lookupKey = selectedKey;
        if (lookupKey != null) {
            selectedConnection.set(connections.stream()
                    .filter(connection -> lookupKey.equals(stableKey(connection)))
                    .findFirst()
                    .orElse(connections.isEmpty() ? null : connections.getFirst()));
        } else {
            selectedConnection.set(connections.isEmpty() ? null : connections.getFirst());
        }
    }

    private static String clearSelectedIfRemoved(String selectedKey, JvmConnection removed) {
        return Objects.equals(selectedKey, stableKey(removed)) ? null : selectedKey;
    }

    private void replaceOrAdd(JvmConnection connection, String status) {
        int index = indexOfStable(connection);
        if (index >= 0) {
            connections.set(index, connection);
        } else {
            connections.add(connection);
        }
        selectedConnection.set(connection);
        statusMessage.set(status);
        clearError();
        finishWork();
    }

    private int indexOfStable(JvmConnection target) {
        String targetKey = stableKey(target);
        for (int i = 0; i < connections.size(); i++) {
            if (Objects.equals(stableKey(connections.get(i)), targetKey)) {
                return i;
            }
        }
        return -1;
    }

    private static String stableKey(JvmConnection connection) {
        if (connection == null) {
            return null;
        }
        if (connection.source() == JvmConnectionSource.LOCAL && !connection.pid().isBlank()) {
            return "local:" + connection.pid();
        }
        return connection.source() + ":" + connection.id();
    }

    private void beginWork() {
        pendingWorkCount++;
        loading.set(true);
        clearError();
    }

    private void finishWork() {
        if (pendingWorkCount > 0) {
            pendingWorkCount--;
        }
        loading.set(pendingWorkCount > 0);
    }

    private void clearError() {
        error.set(false);
        errorMessage.set("");
    }

    private void loadSessionForSelection(JvmConnection connection) {
        long generation = nextSessionLoadGeneration();
        selectedSession.set(null);
        clearRecordingControl();
        clearMBeanBrowser();
        clearSessionError();
        if (connection == null || !connection.connected()) {
            sessionLoading.set(false);
            return;
        }
        sessionLoading.set(true);
        executor.execute(() -> {
            try {
                JvmSessionSnapshot snapshot = connectionService.sessionSnapshot(connection);
                runOnFx(() -> {
                    if (!isCurrentSessionLoad(generation, connection)) {
                        return;
                    }
                    selectedSession.set(snapshot);
                    clearSessionError();
                    loadRecordingControl(snapshot);
                    loadMBeanBrowser(snapshot);
                    sessionLoading.set(false);
                });
            } catch (RuntimeException exception) {
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Unable to load JVM session for {}", connection.displayName());
                runOnFx(() -> {
                    if (!isCurrentSessionLoad(generation, connection)) {
                        return;
                    }
                    selectedSession.set(null);
                    clearRecordingControl();
                    clearMBeanBrowser();
                    sessionError.set(true);
                    sessionErrorMessage.set(exception.getMessage() == null
                            ? exception.getClass().getSimpleName() : exception.getMessage());
                    sessionLoading.set(false);
                });
            }
        });
    }

    private void clearSessionError() {
        sessionError.set(false);
        sessionErrorMessage.set("");
    }

    private void loadRecordingControl(JvmSessionSnapshot snapshot) {
        if (flightRecordingService == null
                || snapshot.statusOf(JvmCapability.FLIGHT_RECORDER) != JvmCapabilityStatus.AVAILABLE
                || !flightRecordingService.isRecordingControlAvailable(snapshot.connection())) {
            clearRecordingControl();
            return;
        }
        try {
            List<FlightRecordingInfo> recordings = flightRecordingService.recordings(snapshot.connection());
            flightRecordings.setAll(recordings);
            selectedFlightRecording.set(recordings.isEmpty() ? null : recordings.getFirst());
            recordingControlAvailable.set(true);
            clearRecordingError();
        } catch (RuntimeException exception) {
            LOGGER.atError()
                    .withThrowable(exception)
                    .log("Unable to load Flight Recorder state for {}", snapshot.connection().displayName());
            clearRecordingControl();
            recordingError.set(true);
            recordingErrorMessage.set(exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private boolean canUseRecordingControl(JvmConnection connection) {
        return flightRecordingService != null && connection != null && connection.connected()
                && recordingControlAvailable.get();
    }

    private void loadMBeanBrowser(JvmSessionSnapshot snapshot) {
        if (!canUseMBeanBrowser(snapshot)) {
            clearMBeanBrowser();
            return;
        }
        long generation = nextMBeanRequestGeneration();
        mbeanBrowserAvailable.set(true);
        mbeanLoading.set(true);
        clearMBeanError();
        clearMBeanDetails();
        executor.execute(() -> {
            try {
                List<MBeanNode> tree = mBeanBrowserService.tree(snapshot.connection());
                runOnFx(() -> {
                    if (!isCurrentMBeanSessionRequest(generation, snapshot)) {
                        return;
                    }
                    mbeanTree.setAll(tree);
                    mbeanLoading.set(false);
                    clearMBeanError();
                });
            } catch (RuntimeException exception) {
                failMBean(generation, snapshot, null, null, exception);
            }
        });
    }

    private void loadSelectedMBeanDetails(MBeanNode node) {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (!canUseMBeanBrowser(snapshot) || node == null || node.domain()) {
            nextMBeanRequestGeneration();
            clearMBeanDetails();
            mbeanOperationResult.set("");
            mbeanLoading.set(false);
            clearMBeanError();
            return;
        }
        clearMBeanDetails();
        mbeanOperationResult.set("");
        long generation = nextMBeanRequestGeneration();
        mbeanLoading.set(true);
        clearMBeanError();
        executor.execute(() -> {
            try {
                List<MBeanAttributeInfo> attributes = mBeanBrowserService.attributes(snapshot.connection(),
                        node.objectName());
                List<MBeanOperationInfo> operations = mBeanBrowserService.operations(snapshot.connection(),
                        node.objectName());
                runOnFx(() -> {
                    if (!isCurrentMBeanRequest(generation, snapshot, node)) {
                        return;
                    }
                    mbeanAttributes.setAll(attributes);
                    mbeanOperations.setAll(operations);
                    selectedMBeanOperation.set(operations.isEmpty() ? null : operations.getFirst());
                    mbeanLoading.set(false);
                    clearMBeanError();
                });
            } catch (RuntimeException exception) {
                failMBean(generation, snapshot, node, null, exception);
            }
        });
    }

    private boolean canUseMBeanBrowser(JvmSessionSnapshot snapshot) {
        return mBeanBrowserService != null && snapshot != null
                && snapshot.statusOf(JvmCapability.MBEAN_SERVER) == JvmCapabilityStatus.AVAILABLE;
    }

    private void discardSessionStartedRecordings() {
        if (flightRecordingService == null || sessionStartedRecordings.isEmpty()) {
            return;
        }
        Map<Long, JvmConnection> started = Map.copyOf(sessionStartedRecordings);
        sessionStartedRecordings.clear();
        for (Map.Entry<Long, JvmConnection> entry : started.entrySet()) {
            try {
                if (isStillRunning(entry.getValue(), entry.getKey())) {
                    flightRecordingService.stopAndDiscardRecording(entry.getValue(), entry.getKey());
                }
            } catch (RuntimeException exception) {
                LOGGER.atWarn()
                        .withThrowable(exception)
                        .log("Unable to discard Flight Recorder recording {} on {}",
                                entry.getKey(), entry.getValue().displayName());
            }
        }
    }

    private boolean isStillRunning(JvmConnection connection, long recordingId) {
        return flightRecordingService.recordings(connection).stream()
                .anyMatch(recording -> recording.id() == recordingId
                        && recording.state() == FlightRecordingState.RUNNING);
    }

    private static String recordingName(JvmConnection connection) {
        String id = connection.pid().isBlank() ? connection.id() : connection.pid();
        String safeId = id.replaceAll("[^A-Za-z0-9]+", "");
        if (safeId.isBlank()) {
            safeId = "jvm";
        }
        return "jmcfx-" + safeId + "-" + LocalDateTime.now(ZoneOffset.UTC).format(RECORDING_NAME_TIMESTAMP);
    }

    private void clearRecordingControl() {
        flightRecordings.clear();
        selectedFlightRecording.set(null);
        recordingControlAvailable.set(false);
        recordingLoading.set(false);
        recordingStatusMessage.set("");
        clearRecordingError();
    }

    private void clearRecordingError() {
        recordingError.set(false);
        recordingErrorMessage.set("");
    }

    private void clearMBeanBrowser() {
        nextMBeanRequestGeneration();
        mbeanTree.clear();
        selectedMBean.set(null);
        mbeanBrowserAvailable.set(false);
        mbeanLoading.set(false);
        mbeanOperationArguments.set("");
        mbeanOperationResult.set("");
        clearMBeanDetails();
        clearMBeanError();
    }

    private void clearMBeanDetails() {
        mbeanAttributes.clear();
        mbeanOperations.clear();
        selectedMBeanOperation.set(null);
    }

    private void clearMBeanError() {
        mbeanError.set(false);
        mbeanErrorMessage.set("");
    }

    private void clearMBeanOperationResult() {
        nextMBeanRequestGeneration();
        mbeanOperationResult.set("");
        mbeanLoading.set(false);
        clearMBeanError();
    }

    private void failRecording(RuntimeException exception) {
        LOGGER.error("Flight Recorder action failed", exception);
        runOnFx(() -> {
            recordingError.set(true);
            recordingErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            recordingLoading.set(false);
        });
    }

    private void failMBean(long generation, JvmSessionSnapshot snapshot, MBeanNode node,
            MBeanOperationInfo operation, RuntimeException exception) {
        LOGGER.error("MBean browser action failed", exception);
        runOnFx(() -> {
            if (node == null) {
                if (!isCurrentMBeanSessionRequest(generation, snapshot)) {
                    return;
                }
            } else if (operation == null && !isCurrentMBeanRequest(generation, snapshot, node)) {
                return;
            } else if (operation != null && !isCurrentMBeanRequest(generation, snapshot, node, operation)) {
                return;
            }
            mbeanError.set(true);
            mbeanErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            mbeanLoading.set(false);
        });
    }

    private void fail(RuntimeException exception) {
        LOGGER.error("JVM browser action failed", exception);
        runOnFx(() -> {
            finishWork();
            error.set(true);
            errorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
        });
    }

    private void runOnFx(Runnable runnable) {
        fxRunner.accept(runnable);
    }

    private long nextSessionLoadGeneration() {
        return ++sessionLoadGeneration;
    }

    private boolean isCurrentSessionLoad(long generation, JvmConnection connection) {
        return sessionLoadGeneration == generation && selectedConnection.get() == connection
                && connection != null && connection.connected();
    }

    private long nextMBeanRequestGeneration() {
        return ++mbeanRequestGeneration;
    }

    private boolean isCurrentMBeanSessionRequest(long generation, JvmSessionSnapshot snapshot) {
        return mbeanRequestGeneration == generation && selectedSession.get() == snapshot && mbeanBrowserAvailable.get();
    }

    private boolean isCurrentMBeanRequest(long generation, JvmSessionSnapshot snapshot, MBeanNode node) {
        return isCurrentMBeanSessionRequest(generation, snapshot) && selectedMBean.get() == node;
    }

    private boolean isCurrentMBeanRequest(long generation, JvmSessionSnapshot snapshot, MBeanNode node,
            MBeanOperationInfo operation) {
        return isCurrentMBeanRequest(generation, snapshot, node) && selectedMBeanOperation.get() == operation;
    }

    private static List<String> parseMBeanArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return List.of();
        }
        return List.of(arguments.split(",", -1)).stream()
                .map(String::trim)
                .toList();
    }

    private static String displayMBeanResult(MBeanOperationResult result) {
        if (!result.error().isBlank()) {
            return result.error();
        }
        return result.value();
    }
}
