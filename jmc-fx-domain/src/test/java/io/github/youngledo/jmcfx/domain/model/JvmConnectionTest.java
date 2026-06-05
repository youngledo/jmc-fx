package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JvmConnectionTest {

    @Test
    void legacyConstructorDefaultsToManualDisconnectedMetadata() {
        JvmConnection connection = new JvmConnection("id-1", "Demo", "", false);

        assertEquals("id-1", connection.id());
        assertEquals("Demo", connection.displayName());
        assertEquals("", connection.pid());
        assertEquals("", connection.javaVersion());
        assertFalse(connection.attachable());
        assertEquals(JvmConnectionSource.MANUAL, connection.source());
        assertEquals(JvmConnectionState.DISCONNECTED, connection.state());
    }

    @Test
    void localAttachableFactorySetsPidAndAttachableState() {
        JvmConnection connection = JvmConnection.local("123", "demo.Main", "26.0.1", true);

        assertEquals("123", connection.id());
        assertEquals("123", connection.pid());
        assertEquals("demo.Main", connection.displayName());
        assertEquals("26.0.1", connection.javaVersion());
        assertTrue(connection.attachable());
        assertFalse(connection.connected());
        assertEquals(JvmConnectionSource.LOCAL, connection.source());
        assertEquals(JvmConnectionState.ATTACHABLE, connection.state());
    }

    @Test
    void localUnavailableFactorySetsUnavailableState() {
        JvmConnection connection = JvmConnection.local("456", "blocked.Main", "", false);

        assertEquals("456", connection.pid());
        assertFalse(connection.attachable());
        assertEquals(JvmConnectionState.UNAVAILABLE, connection.state());
    }

    @Test
    void connectedLocalRowKeepsLocalMetadata() {
        JvmConnection discovered = JvmConnection.local("123", "demo.Main", "26.0.1", true);

        JvmConnection connected = discovered.asConnected("service:jmx:local://123");

        assertEquals("123", connected.pid());
        assertEquals("26.0.1", connected.javaVersion());
        assertTrue(connected.attachable());
        assertTrue(connected.connected());
        assertEquals(JvmConnectionSource.LOCAL, connected.source());
        assertEquals(JvmConnectionState.CONNECTED, connected.state());
    }
}
