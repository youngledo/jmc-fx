package com.youngledo.jmcfx.application;

import java.util.Objects;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;

public final class LiveJvmConnectionUseCase {

    private final JmxConnectionService service;

    public LiveJvmConnectionUseCase(JmxConnectionService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public JvmConnection connect(String connectionUrl) {
        return service.connect(connectionUrl);
    }

    public JvmConnection connectLocal(JvmConnection localConnection) {
        return service.connectLocal(localConnection);
    }

    public JvmSessionSnapshot sessionSnapshot(JvmConnection connection) {
        return service.sessionSnapshot(connection);
    }

    public void disconnect(JvmConnection connection) {
        service.disconnect(connection);
    }
}
