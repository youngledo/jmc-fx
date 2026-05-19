package com.youngledo.jmcfx.adapter.jmc;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;

/// JMC-backed JVM discovery boundary.
///
/// Discovery remains disabled until the exact OpenJDK JMC API surface is
/// verified for this standalone JavaFX application.
public class JmcJvmDiscoveryService implements JvmDiscoveryService {

    @Override
    public List<JvmConnection> discoverLocalJvms() {
        return List.of();
    }
}
