package com.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record SavedJvmTarget(String id, String displayName, String serviceUrl, Instant lastConnectedAt) {

    public SavedJvmTarget {
        id = Objects.requireNonNullElse(id, "").trim();
        displayName = Objects.requireNonNullElse(displayName, "").trim();
        serviceUrl = Objects.requireNonNullElse(serviceUrl, "").trim();
    }

    public SavedJvmTarget withLastConnectedAt(Instant connectedAt) {
        return new SavedJvmTarget(id, displayName, serviceUrl, connectedAt);
    }
}
