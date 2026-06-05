package io.github.youngledo.jmcfx.application;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.JvmDiscoveryService;

public final class LiveJvmDiscoveryUseCase {

    private final JvmDiscoveryService jvmDiscoveryService;
    private final JdpDiscoveryService jdpDiscoveryService;

    public LiveJvmDiscoveryUseCase(JvmDiscoveryService jvmDiscoveryService, JdpDiscoveryService jdpDiscoveryService) {
        this.jvmDiscoveryService = Objects.requireNonNull(jvmDiscoveryService, "jvmDiscoveryService");
        this.jdpDiscoveryService = jdpDiscoveryService;
    }

    public List<JvmConnection> discoverLocalJvms() {
        return jvmDiscoveryService.discoverLocalJvms();
    }

    public boolean jdpAvailable() {
        return jdpDiscoveryService != null;
    }

    public List<JdpJvmAdvertisement> discoverJdp(Duration timeout) {
        return jdpDiscoveryService.discover(timeout);
    }
}
