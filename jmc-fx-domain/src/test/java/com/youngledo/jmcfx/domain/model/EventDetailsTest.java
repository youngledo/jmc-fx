package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventDetailsTest {

    @Test
    void exposesStructuredDetails() {
        EventDetails details = new EventDetails("jdk.CPULoad#0", "jdk.CPULoad",
                List.of(new EventProperty("jvmUser", "JVM User", "0.12", "", "User CPU load")),
                new EventTiming(Instant.EPOCH, Instant.EPOCH.plusMillis(1), 1_000_000, "1 ms", "0 ms"),
                new EventThreadInfo("JVM Periodic Tasks", "7", false),
                List.of(new EventStackFrame("com.example.App", "run", "App.java", 42)));

        assertEquals("jdk.CPULoad#0", details.eventId());
        assertEquals("JVM User", details.properties().getFirst().label());
        assertEquals("com.example.App", details.stackTrace().getFirst().typeName());
    }
}
