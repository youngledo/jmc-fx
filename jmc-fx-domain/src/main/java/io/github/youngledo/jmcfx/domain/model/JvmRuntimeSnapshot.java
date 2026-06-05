package io.github.youngledo.jmcfx.domain.model;

import java.time.Instant;
import java.util.Objects;

public record JvmRuntimeSnapshot(
        String vmName,
        String vmVendor,
        String vmVersion,
        String specVersion,
        Instant startTime,
        long uptimeMillis) {

    public JvmRuntimeSnapshot {
        vmName = Objects.requireNonNullElse(vmName, "");
        vmVendor = Objects.requireNonNullElse(vmVendor, "");
        vmVersion = Objects.requireNonNullElse(vmVersion, "");
        specVersion = Objects.requireNonNullElse(specVersion, "");
        startTime = startTime == null ? Instant.EPOCH : startTime;
        uptimeMillis = Math.max(0, uptimeMillis);
    }

    public static JvmRuntimeSnapshot empty() {
        return new JvmRuntimeSnapshot("", "", "", "", Instant.EPOCH, 0);
    }
}
