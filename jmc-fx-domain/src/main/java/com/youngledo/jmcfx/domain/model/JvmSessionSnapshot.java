package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record JvmSessionSnapshot(
        JvmConnection connection,
        JvmRuntimeSnapshot runtime,
        List<JvmCapabilitySnapshot> capabilities) {

    public JvmSessionSnapshot {
        connection = Objects.requireNonNull(connection, "connection");
        runtime = Objects.requireNonNull(runtime, "runtime");
        capabilities = List.copyOf(Objects.requireNonNullElse(capabilities, List.of()));
    }

    public JvmCapabilityStatus statusOf(JvmCapability capability) {
        return capabilities.stream()
                .filter(snapshot -> snapshot.capability() == capability)
                .map(JvmCapabilitySnapshot::status)
                .findFirst()
                .orElse(JvmCapabilityStatus.UNKNOWN);
    }
}
