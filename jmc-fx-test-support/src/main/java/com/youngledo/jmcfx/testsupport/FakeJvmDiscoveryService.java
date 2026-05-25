package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;

public class FakeJvmDiscoveryService implements JvmDiscoveryService {

    private final List<JvmConnection> connections = new ArrayList<>();
    private RuntimeException failure;

    public void add(JvmConnection connection) {
        connections.add(connection);
    }

    public void setConnections(List<JvmConnection> nextConnections) {
        connections.clear();
        connections.addAll(nextConnections);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public List<JvmConnection> discoverLocalJvms() {
        if (failure != null) {
            throw failure;
        }
        return List.copyOf(connections);
    }
}
