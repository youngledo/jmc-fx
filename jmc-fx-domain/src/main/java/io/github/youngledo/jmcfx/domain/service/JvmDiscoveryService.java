package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;

public interface JvmDiscoveryService {
    List<JvmConnection> discoverLocalJvms();
}
