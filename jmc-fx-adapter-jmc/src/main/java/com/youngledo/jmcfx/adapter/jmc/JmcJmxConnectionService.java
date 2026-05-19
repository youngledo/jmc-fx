package com.youngledo.jmcfx.adapter.jmc;

import java.util.UUID;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;

/// JMC-backed JMX connection boundary.
///
/// This placeholder preserves the application port while live JMX integration
/// is verified against OpenJDK JMC headless APIs.
public class JmcJmxConnectionService implements JmxConnectionService {

    @Override
    public JvmConnection connect(String connectionUrl) {
        return new JvmConnection(UUID.randomUUID().toString(), connectionUrl, connectionUrl, true);
    }

    @Override
    public void disconnect(JvmConnection connection) {
    }
}
