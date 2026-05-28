package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.service.JmcFxException;

class JmcJdpDiscoveryServiceTest {

    @Test
    void parsesValidAdvertisementPayload() {
        String payload = """
                DISCOVERABLE_SESSION_UUID=8f2b927e-2ac2-4f58-b36e-e0d99492fb7b
                INSTANCE_NAME=JMC FX Demo
                MAIN_CLASS=com.example.Demo
                JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://demo.example.com:7091/jmxrmi
                JVM_VERSION=26.0.1
                """;

        var advertisement = JmcJdpDiscoveryService.parseAdvertisement(payload, "192.0.2.10").orElseThrow();

        assertEquals("8f2b927e-2ac2-4f58-b36e-e0d99492fb7b", advertisement.id());
        assertEquals("JMC FX Demo", advertisement.displayName());
        assertEquals("service:jmx:rmi:///jndi/rmi://demo.example.com:7091/jmxrmi", advertisement.serviceUrl());
        assertEquals("192.0.2.10", advertisement.host());
        assertEquals(7091, advertisement.port());
        assertEquals("26.0.1", advertisement.javaVersion());
    }

    @Test
    void skipsPayloadMissingJmxServiceUrl() {
        String payload = """
                DISCOVERABLE_SESSION_UUID=8f2b927e-2ac2-4f58-b36e-e0d99492fb7b
                INSTANCE_NAME=JMC FX Demo
                """;

        assertTrue(JmcJdpDiscoveryService.parseAdvertisement(payload, "192.0.2.10").isEmpty());
    }

    @Test
    void skipsPayloadWithBlankJmxServiceUrl() {
        String payload = """
                DISCOVERABLE_SESSION_UUID=8f2b927e-2ac2-4f58-b36e-e0d99492fb7b
                JMX_SERVICE_URL=
                """;

        assertTrue(JmcJdpDiscoveryService.parseAdvertisement(payload, "192.0.2.10").isEmpty());
    }

    @Test
    void fallsBackDisplayNameToMainClassThenServiceUrl() {
        String mainClassPayload = """
                MAIN_CLASS=com.example.Demo
                JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://demo.example.com:7091/jmxrmi
                """;
        String serviceUrlPayload = """
                JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://demo.example.com:7092/jmxrmi
                """;

        var mainClassAdvertisement = JmcJdpDiscoveryService.parseAdvertisement(mainClassPayload, "192.0.2.10")
                .orElseThrow();
        var serviceUrlAdvertisement = JmcJdpDiscoveryService.parseAdvertisement(serviceUrlPayload, "192.0.2.10")
                .orElseThrow();

        assertEquals("com.example.Demo", mainClassAdvertisement.displayName());
        assertEquals("service:jmx:rmi:///jndi/rmi://demo.example.com:7092/jmxrmi",
                serviceUrlAdvertisement.displayName());
    }

    @Test
    void fallsBackIdToServiceUrl() {
        String payload = """
                INSTANCE_NAME=JMC FX Demo
                JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://demo.example.com:7091/jmxrmi
                """;

        var advertisement = JmcJdpDiscoveryService.parseAdvertisement(payload, "192.0.2.10").orElseThrow();

        assertEquals("service:jmx:rmi:///jndi/rmi://demo.example.com:7091/jmxrmi", advertisement.id());
    }

    @Test
    void fallsBackPortToNegativeOneWhenServiceUrlHasNoPort() {
        String payload = """
                INSTANCE_NAME=JMC FX Demo
                JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://demo.example.com/jmxrmi
                """;

        var advertisement = JmcJdpDiscoveryService.parseAdvertisement(payload, "192.0.2.10").orElseThrow();

        assertEquals(-1, advertisement.port());
    }

    @Test
    void normalizesNullAndNonPositiveTimeoutToDefaultDiscoveryTimeout() {
        assertEquals(Duration.ofMillis(750), JmcJdpDiscoveryService.normalizeTimeout(null));
        assertEquals(Duration.ofMillis(750), JmcJdpDiscoveryService.normalizeTimeout(Duration.ZERO));
        assertEquals(Duration.ofMillis(750), JmcJdpDiscoveryService.normalizeTimeout(Duration.ofMillis(-1)));
        assertEquals(Duration.ofMillis(25), JmcJdpDiscoveryService.normalizeTimeout(Duration.ofMillis(25)));
    }

    @Test
    void discoversDeduplicatedAdvertisementsSortedByDisplayName() {
        JmcJdpDiscoveryService service = new JmcJdpDiscoveryService(timeout -> List.of(
                new JmcJdpDiscoveryService.ReceivedJdpPayload("""
                        DISCOVERABLE_SESSION_UUID=id-2
                        INSTANCE_NAME=Zulu
                        JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://example.com:7002/jmxrmi
                        """, "192.0.2.2"),
                new JmcJdpDiscoveryService.ReceivedJdpPayload("""
                        DISCOVERABLE_SESSION_UUID=id-1
                        INSTANCE_NAME=Alpha
                        JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://example.com:7001/jmxrmi
                        """, "192.0.2.1"),
                new JmcJdpDiscoveryService.ReceivedJdpPayload("""
                        DISCOVERABLE_SESSION_UUID=id-1
                        INSTANCE_NAME=Duplicate Id
                        JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://example.com:7011/jmxrmi
                        """, "192.0.2.11"),
                new JmcJdpDiscoveryService.ReceivedJdpPayload("""
                        DISCOVERABLE_SESSION_UUID=id-3
                        INSTANCE_NAME=Duplicate Url
                        JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://example.com:7002/jmxrmi
                        """, "192.0.2.3")));

        var advertisements = service.discover(Duration.ofMillis(25));

        assertEquals(2, advertisements.size());
        assertEquals("Alpha", advertisements.getFirst().displayName());
        assertEquals("id-1", advertisements.getFirst().id());
        assertEquals("Zulu", advertisements.get(1).displayName());
        assertEquals("id-2", advertisements.get(1).id());
    }

    @Test
    void wrapsReceiverIoFailureInJmcFxException() {
        IOException ioFailure = new IOException("cannot bind socket");
        JmcJdpDiscoveryService service = new JmcJdpDiscoveryService(timeout -> {
            throw ioFailure;
        });

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.discover(Duration.ofMillis(25)));

        assertEquals("JDP discovery failed", exception.getMessage());
        assertSame(ioFailure, exception.getCause());
    }
}
