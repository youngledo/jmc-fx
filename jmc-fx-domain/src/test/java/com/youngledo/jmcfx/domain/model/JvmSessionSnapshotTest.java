package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class JvmSessionSnapshotTest {

    @Test
    void normalizesNullStringsAndCopiesCapabilities() {
        JvmConnection connection = JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
        JvmRuntimeSnapshot runtime = new JvmRuntimeSnapshot(null, "Vendor", "26.0.1", "26",
                Instant.EPOCH, 1234);
        JvmSessionSnapshot snapshot = new JvmSessionSnapshot(connection, runtime, List.of(
                new JvmCapabilitySnapshot(JvmCapability.FLIGHT_RECORDER,
                        JvmCapabilityStatus.AVAILABLE, null)));

        assertEquals("", snapshot.runtime().vmName());
        assertEquals(JvmCapabilityStatus.AVAILABLE,
                snapshot.statusOf(JvmCapability.FLIGHT_RECORDER));
        assertEquals(JvmCapabilityStatus.UNKNOWN,
                snapshot.statusOf(JvmCapability.DIAGNOSTIC_COMMANDS));

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.capabilities().add(new JvmCapabilitySnapshot(
                        JvmCapability.MBEAN_SERVER, JvmCapabilityStatus.AVAILABLE, "")));
    }

    @Test
    void rejectsMissingConnectionAndRuntime() {
        assertThrows(NullPointerException.class,
                () -> new JvmSessionSnapshot(null, JvmRuntimeSnapshot.empty(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new JvmSessionSnapshot(new JvmConnection("id", "name", "", false),
                        null, List.of()));
    }
}
