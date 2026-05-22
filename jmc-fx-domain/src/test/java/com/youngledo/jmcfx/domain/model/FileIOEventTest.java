package com.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileIOEventTest {

    @Test
    void recordComponentsAreAccessible() {
        FileIOEvent event = new FileIOEvent("jdk.FileRead", "/tmp/data.bin",
                1024, 2.5, 1700000000000L, "main");
        assertEquals("jdk.FileRead", event.eventType());
        assertEquals("/tmp/data.bin", event.path());
        assertEquals(1024, event.bytes());
        assertEquals(2.5, event.durationMillis());
        assertEquals(1700000000000L, event.timestamp());
        assertEquals("main", event.threadName());
    }
}
