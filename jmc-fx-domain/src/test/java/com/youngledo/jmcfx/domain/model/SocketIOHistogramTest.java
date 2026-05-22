package com.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIOHistogramTest {

    @Test
    void recordComponentsAreAccessible() {
        SocketIOHistogram row = new SocketIOHistogram("10.0.0.1:8080",
                "10.0.0.1", 8080, 50, 30, 102400, 51200, 300, 25, 3.75);
        assertEquals("10.0.0.1:8080", row.key());
        assertEquals("10.0.0.1", row.host());
        assertEquals(8080, row.port());
        assertEquals(50, row.readCount());
        assertEquals(30, row.writeCount());
    }
}
