package io.github.youngledo.jmcfx.domain.service;

import java.time.Duration;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;

public interface JdpDiscoveryService {

    List<JdpJvmAdvertisement> discover(Duration timeout);
}
