package com.youngledo.jmcfx.ui.jvms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
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

    private final JvmDiscoveryService discoveryService;
    private final JmxConnectionService connectionService;
    private final JvmBrowserExecutor executor;
    private final Consumer<Runnable> fxRunner;
    private final ObservableList<JvmConnection> connections = FXCollections.observableArrayList();
    private final ObjectProperty<JvmConnection> selectedConnection = new SimpleObjectProperty<>();
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
    private int pendingWorkCount;

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService) {
        this(discoveryService, connectionService, new VirtualThreadJvmBrowserExecutor(),
                javafx.application.Platform::runLater);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner) {
        this.discoveryService = Objects.requireNonNull(discoveryService, "discoveryService");
        this.connectionService = Objects.requireNonNull(connectionService, "connectionService");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.fxRunner = Objects.requireNonNull(fxRunner, "fxRunner");
        this.selectedConnection.addListener((observable, oldValue, newValue) -> loadSessionForSelection(newValue));
    }

    public ObservableList<JvmConnection> connectionsProperty() {
        return connections;
    }

    public ObjectProperty<JvmConnection> selectedConnectionProperty() {
        return selectedConnection;
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
                    clearSessionError();
                });
            } catch (RuntimeException exception) {
                fail(exception);
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
                    sessionLoading.set(false);
                });
            } catch (RuntimeException exception) {
                runOnFx(() -> {
                    selectedSession.set(null);
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

    private void fail(RuntimeException exception) {
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
