package com.youngledo.jmcfx.domain.service;

import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface JmxConnectionService {
    JvmConnection connect(String connectionUrl);

    default JvmConnection connectLocal(JvmConnection localConnection) {
        throw new JmcFxException("Local JVM attach connection is not supported by this service.");
    }

    void disconnect(JvmConnection connection);
}
