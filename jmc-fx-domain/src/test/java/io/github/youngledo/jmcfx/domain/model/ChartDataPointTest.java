package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChartDataPointTest {
    @Test
    void construction() {
        ChartDataPoint point = new ChartDataPoint(1.0, 2.5);
        assertEquals(1.0, point.x());
        assertEquals(2.5, point.y());
    }

    @Test
    void equality() {
        assertEquals(new ChartDataPoint(1.0, 2.0), new ChartDataPoint(1.0, 2.0));
    }
}
