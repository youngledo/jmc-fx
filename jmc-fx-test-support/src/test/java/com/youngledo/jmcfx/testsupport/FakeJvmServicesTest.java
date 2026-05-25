package com.youngledo.jmcfx.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;

class FakeJvmServicesTest {

    @Test
    void fakeDiscoveryCanReplaceSnapshot() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(local("1", "one.Main")));
        discovery.setConnections(List.of(local("2", "two.Main")));

        assertEquals(1, discovery.discoverLocalJvms().size());
        assertEquals("2", discovery.discoverLocalJvms().getFirst().pid());
    }

    @Test
    void fakeJmxServiceConnectsRemoteUrl() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JvmConnection connected = service.connect("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        assertTrue(connected.connected());
        assertEquals(JvmConnectionSource.MANUAL, connected.source());
        assertEquals(JvmConnectionState.CONNECTED, connected.state());
        assertTrue(service.connectedConnections().contains(connected.id()));
    }

    @Test
    void fakeJmxServiceConnectsLocalAttachableJvm() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JvmConnection connected = service.connectLocal(local("42", "demo.Main"));

        assertTrue(connected.connected());
        assertEquals("42", connected.id());
        assertEquals("42", connected.pid());
        assertEquals(JvmConnectionSource.LOCAL, connected.source());
        assertEquals("service:jmx:local://42", connected.connectionUrl());
        assertTrue(service.connectedConnections().contains(connected.id()));
    }

    @Test
    void fakeJmxServiceRejectsUnavailableLocalJvm() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.connectLocal(JvmConnection.local("42", "blocked.Main", "", false)));
    }

    @Test
    void fakeJmxServiceRejectsLocalJvmWithoutPid() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.connectLocal(JvmConnection.local("", "unknown.Main", "26.0.1", true)));
    }

    private static JvmConnection local(String pid, String name) {
        return JvmConnection.local(pid, name, "26.0.1", true);
    }
}
