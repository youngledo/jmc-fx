package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.JvmConnectionSource;
import io.github.youngledo.jmcfx.domain.model.JvmConnectionState;
import io.github.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.JmxConnectionService;

public class FakeJmxConnectionService implements JmxConnectionService {

    private final Set<String> connectedConnections = new HashSet<>();
    private final Map<String, JvmSessionSnapshot> sessionSnapshots = new HashMap<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public Set<String> connectedConnections() {
        return Set.copyOf(connectedConnections);
    }

    public void setSessionSnapshot(String connectionId, JvmSessionSnapshot snapshot) {
        sessionSnapshots.put(connectionId, snapshot);
    }

    @Override
    public JvmConnection connect(String connectionUrl) {
        if (failure != null) {
            throw failure;
        }
        JvmConnection connection = new JvmConnection(UUID.randomUUID().toString(),
                connectionUrl, connectionUrl, true, JvmConnectionSource.MANUAL,
                JvmConnectionState.CONNECTED, "Connected");
        connectedConnections.add(connection.id());
        return connection;
    }

    @Override
    public JvmConnection connectLocal(JvmConnection localConnection) {
        if (failure != null) {
            throw failure;
        }
        if (localConnection == null || localConnection.source() != JvmConnectionSource.LOCAL
                || !localConnection.attachable() || localConnection.pid().isBlank()) {
            throw new IllegalArgumentException("Local JVM must be attachable and have a PID.");
        }
        JvmConnection connected = localConnection.asConnected("service:jmx:local://" + localConnection.pid());
        connectedConnections.add(connected.id());
        return connected;
    }

    @Override
    public JvmSessionSnapshot sessionSnapshot(JvmConnection connection) {
        if (failure != null) {
            throw failure;
        }
        String id = connection == null ? "" : connection.id();
        JvmSessionSnapshot snapshot = sessionSnapshots.get(id);
        if (snapshot == null) {
            throw new JmcFxException("No live JVM session for connection: " + id);
        }
        return snapshot;
    }

    @Override
    public void disconnect(JvmConnection connection) {
        if (connection != null) {
            connectedConnections.remove(connection.id());
            sessionSnapshots.remove(connection.id());
        }
    }
}
