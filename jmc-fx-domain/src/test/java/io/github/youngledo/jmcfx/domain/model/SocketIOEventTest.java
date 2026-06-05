package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIOEventTest {

    @Test
    void recordComponentsAreAccessible() {
        SocketIOEvent event = new SocketIOEvent("jdk.SocketRead", "10.0.0.1",
                8080, 2048, 5000, 12.5, 1700000000000L, "http-nio-8080-exec-1");
        assertEquals("jdk.SocketRead", event.eventType());
        assertEquals("10.0.0.1", event.host());
        assertEquals(8080, event.port());
        assertEquals(2048, event.bytes());
        assertEquals(5000, event.timeout());
    }
}
