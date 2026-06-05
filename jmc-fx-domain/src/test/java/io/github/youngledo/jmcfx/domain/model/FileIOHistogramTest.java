package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileIOHistogramTest {

    @Test
    void recordComponentsAreAccessible() {
        FileIOHistogram row = new FileIOHistogram("/var/log/app.log",
                10, 5, 4096, 2048, 150, 50, 10.0);
        assertEquals("/var/log/app.log", row.path());
        assertEquals(10, row.readCount());
        assertEquals(5, row.writeCount());
        assertEquals(4096, row.readSize());
        assertEquals(2048, row.writeSize());
        assertEquals(150, row.totalDuration());
        assertEquals(50, row.maxDuration());
        assertEquals(10.0, row.avgDuration());
    }
}
