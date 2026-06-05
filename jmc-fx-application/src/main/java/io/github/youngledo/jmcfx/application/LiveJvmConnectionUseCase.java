package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import io.github.youngledo.jmcfx.domain.service.JmxConnectionService;

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
