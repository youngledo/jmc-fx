package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyValueEntryTest {
    @Test
    void construction() {
        KeyValueEntry entry = new KeyValueEntry("JVM Version", "17.0.1");
        assertEquals("JVM Version", entry.label());
        assertEquals("17.0.1", entry.value());
    }
}
