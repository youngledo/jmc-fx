package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class JvmTargetModelTest {

    @Test
    void savedTargetNormalizesTextAndCreatesSavedConnection() {
        SavedJvmTarget target = new SavedJvmTarget(" target-1 ", " Orders API ",
                " service:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi ", Instant.parse("2026-05-28T10:15:30Z"));

        assertEquals("target-1", target.id());
        assertEquals("Orders API", target.displayName());
        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi", target.serviceUrl());
        assertEquals(Instant.parse("2026-05-28T10:15:30Z"), target.lastConnectedAt());

        JvmConnection connection = JvmConnection.saved(target);
        assertEquals(JvmConnectionSource.SAVED, connection.source());
        assertEquals(JvmConnectionState.DISCONNECTED, connection.state());
        assertEquals("Orders API", connection.displayName());
        assertEquals(target.serviceUrl(), connection.connectionUrl());
        assertEquals("Saved JMX target.", connection.statusMessage());
    }

    @Test
    void savedConnectionFallsBackToUrlWhenDisplayNameIsBlank() {
        SavedJvmTarget target = new SavedJvmTarget(" saved-blank ", "  ",
                " service:jmx:rmi:///jndi/rmi://localhost:9011/jmxrmi ", null);

        JvmConnection connection = JvmConnection.saved(target);

        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9011/jmxrmi", connection.displayName());
        assertEquals("saved-blank", connection.id());
        assertEquals(JvmConnectionSource.SAVED, connection.source());
    }

    @Test
    void savedTargetWithLastConnectedAtReturnsUpdatedCopy() {
        SavedJvmTarget target = new SavedJvmTarget(null, null, null, null);
        Instant connectedAt = Instant.parse("2026-05-28T11:15:30Z");

        SavedJvmTarget updated = target.withLastConnectedAt(connectedAt);

        assertEquals("", updated.id());
        assertEquals("", updated.displayName());
        assertEquals("", updated.serviceUrl());
        assertEquals(connectedAt, updated.lastConnectedAt());
    }

    @Test
    void jdpAdvertisementNormalizesTextAndCreatesJdpConnection() {
        JdpJvmAdvertisement advertisement = new JdpJvmAdvertisement(" inventory@host-a ", " Inventory ",
                " service:jmx:rmi:///jndi/rmi://host-a:7091/jmxrmi ", " host-a ", 7091, " 26.0.1 ");

        JvmConnection connection = JvmConnection.jdp(advertisement);

        assertEquals("inventory@host-a", advertisement.id());
        assertEquals("Inventory", advertisement.displayName());
        assertEquals("service:jmx:rmi:///jndi/rmi://host-a:7091/jmxrmi", advertisement.serviceUrl());
        assertEquals("host-a", advertisement.host());
        assertEquals(7091, advertisement.port());
        assertEquals("26.0.1", advertisement.javaVersion());
        assertEquals(JvmConnectionSource.JDP, connection.source());
        assertEquals(JvmConnectionState.DISCOVERED, connection.state());
        assertEquals("Inventory", connection.displayName());
        assertEquals(advertisement.serviceUrl(), connection.connectionUrl());
        assertEquals("26.0.1", connection.javaVersion());
        assertEquals("Discovered through JDP.", connection.statusMessage());
    }

    @Test
    void jdpConnectionFallsBackToUrlWhenDisplayNameIsBlank() {
        JdpJvmAdvertisement advertisement = new JdpJvmAdvertisement(null, null,
                " service:jmx:rmi:///jndi/rmi://host-b:7091/jmxrmi ", null, 7091, null);

        JvmConnection connection = JvmConnection.jdp(advertisement);

        assertEquals("", advertisement.id());
        assertEquals("", advertisement.displayName());
        assertEquals("service:jmx:rmi:///jndi/rmi://host-b:7091/jmxrmi", connection.displayName());
        assertEquals("", advertisement.host());
        assertEquals("", advertisement.javaVersion());
        assertEquals(JvmConnectionSource.JDP, connection.source());
    }
}
