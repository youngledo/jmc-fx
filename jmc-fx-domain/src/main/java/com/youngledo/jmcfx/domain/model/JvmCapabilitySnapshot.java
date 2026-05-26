package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record JvmCapabilitySnapshot(
        JvmCapability capability,
        JvmCapabilityStatus status,
        String message) {

    public JvmCapabilitySnapshot {
        capability = Objects.requireNonNull(capability, "capability");
        status = status == null ? JvmCapabilityStatus.UNKNOWN : status;
        message = Objects.requireNonNullElse(message, "");
    }
}
