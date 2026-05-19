package com.youngledo.jmcfx.domain.service;

import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface JmxConnectionService {
    JvmConnection connect(String connectionUrl);

    void disconnect(JvmConnection connection);
}
