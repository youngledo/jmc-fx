package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockHistogramTest {

    @Test
    void recordComponentsAreAccessible() {
        LockHistogram row = new LockHistogram("java.lang.Object",
                100, 5000, 200, 50.0, 3, 5, 12);
        assertEquals("java.lang.Object", row.key());
        assertEquals(100, row.count());
        assertEquals(5000, row.totalDuration());
        assertEquals(200, row.maxDuration());
        assertEquals(50.0, row.avgDuration());
        assertEquals(3, row.inflateCount());
        assertEquals(5, row.distinctThreads());
        assertEquals(12, row.distinctAddresses());
    }
}
