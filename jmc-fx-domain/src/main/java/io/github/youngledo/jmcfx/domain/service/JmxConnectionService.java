package io.github.youngledo.jmcfx.domain.service;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.JvmSessionSnapshot;

public interface JmxConnectionService {
    JvmConnection connect(String connectionUrl);

    default JvmConnection connectLocal(JvmConnection localConnection) {
        throw new JmcFxException("Local JVM attach connection is not supported by this service.");
    }

    default JvmSessionSnapshot sessionSnapshot(JvmConnection connection) {
        throw new JmcFxException("Live JVM session snapshots are not supported by this service.");
    }

    void disconnect(JvmConnection connection);
}
