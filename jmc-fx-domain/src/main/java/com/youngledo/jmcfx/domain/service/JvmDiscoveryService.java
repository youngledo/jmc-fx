package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface JvmDiscoveryService {
    List<JvmConnection> discoverLocalJvms();
}
