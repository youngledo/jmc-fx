package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record JdpJvmAdvertisement(
        String id,
        String displayName,
        String serviceUrl,
        String host,
        int port,
        String javaVersion) {

    public JdpJvmAdvertisement {
        id = Objects.requireNonNullElse(id, "").trim();
        displayName = Objects.requireNonNullElse(displayName, "").trim();
        serviceUrl = Objects.requireNonNullElse(serviceUrl, "").trim();
        host = Objects.requireNonNullElse(host, "").trim();
        javaVersion = Objects.requireNonNullElse(javaVersion, "").trim();
    }
}
