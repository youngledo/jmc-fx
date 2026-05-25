package com.youngledo.jmcfx.testsupport;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;

public class FakeJmxConnectionService implements JmxConnectionService {

    private final Set<String> connectedConnections = new HashSet<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public Set<String> connectedConnections() {
        return Set.copyOf(connectedConnections);
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
    public void disconnect(JvmConnection connection) {
        if (connection != null) {
            connectedConnections.remove(connection.id());
        }
    }
}
