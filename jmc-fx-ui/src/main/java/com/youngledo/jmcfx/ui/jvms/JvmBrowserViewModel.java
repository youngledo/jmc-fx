package com.youngledo.jmcfx.ui.jvms;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JvmBrowserViewModel implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmBrowserViewModel.class);

    private final JvmDiscoveryService discoveryService;
    private final JmxConnectionService connectionService;
    private final FlightRecordingService flightRecordingService;
    private final JvmBrowserExecutor executor;
    private final Consumer<Runnable> fxRunner;
    private final Consumer<Path> savedRecordingHandler;
    private final ObservableList<JvmConnection> connections = FXCollections.observableArrayList();
    private final ObservableList<FlightRecordingInfo> flightRecordings = FXCollections.observableArrayList();
    private final ObjectProperty<JvmConnection> selectedConnection = new SimpleObjectProperty<>();
    private final ObjectProperty<FlightRecordingInfo> selectedFlightRecording = new SimpleObjectProperty<>();
    private final StringProperty manualConnectionUrl = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
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
    private int pendingWorkCount;

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService) {
        this(discoveryService, connectionService, null, new VirtualThreadJvmBrowserExecutor(),
                javafx.application.Platform::runLater, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner) {
        this(discoveryService, connectionService, null, executor, fxRunner, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner,
            Consumer<Path> savedRecordingHandler) {
        this.discoveryService = Objects.requireNonNull(discoveryService, "discoveryService");
        this.connectionService = Objects.requireNonNull(connectionService, "connectionService");
        this.flightRecordingService = flightRecordingService;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.fxRunner = Objects.requireNonNull(fxRunner, "fxRunner");
        this.savedRecordingHandler = Objects.requireNonNull(savedRecordingHandler, "savedRecordingHandler");
        this.selectedConnection.addListener((observable, oldValue, newValue) -> loadSessionForSelection(newValue));
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
                        "JMC FX Recording", FlightRecordingTemplate.profile());
                flightRecordingService.startRecording(request);
                List<FlightRecordingInfo> updated = flightRecordingService.recordings(selected);
                runOnFx(() -> {
                    flightRecordings.setAll(updated);
                    selectedFlightRecording.set(updated.isEmpty() ? null : updated.getLast());
                    recordingStatusMessage.set("Recording started.");
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
                List<FlightRecordingInfo> updated = flightRecordingService.recordings(selectedConnection);
                runOnFx(() -> {
                    flightRecordings.setAll(updated);
                    selectedFlightRecording.set(updated.isEmpty() ? null : updated.getFirst());
                    recordingStatusMessage.set("Recording saved: " + saved.getFileName());
                    recordingLoading.set(false);
                    savedRecordingHandler.accept(saved);
                });
            } catch (RuntimeException exception) {
                failRecording(exception);
            }
        });
    }

    @Override
    public void close() {
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
        if (connection == null || !connection.connected()) {
            selectedSession.set(null);
            clearRecordingControl();
            clearSessionError();
            sessionLoading.set(false);
            return;
        }
        sessionLoading.set(true);
        executor.execute(() -> {
            try {
                JvmSessionSnapshot snapshot = connectionService.sessionSnapshot(connection);
                runOnFx(() -> {
                    selectedSession.set(snapshot);
                    clearSessionError();
                    loadRecordingControl(snapshot);
                    sessionLoading.set(false);
                });
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to load JVM session for {}", connection.displayName(), exception);
                runOnFx(() -> {
                    selectedSession.set(null);
                    clearRecordingControl();
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
            LOGGER.error("Unable to load Flight Recorder state for {}", snapshot.connection().displayName(), exception);
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

    private void failRecording(RuntimeException exception) {
        LOGGER.error("Flight Recorder action failed", exception);
        runOnFx(() -> {
            recordingError.set(true);
            recordingErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            recordingLoading.set(false);
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
}
