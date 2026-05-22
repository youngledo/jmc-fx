package com.youngledo.jmcfx.domain.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChartSeriesTest {
    @Test
    void construction() {
        List<ChartDataPoint> points = List.of(new ChartDataPoint(0, 1), new ChartDataPoint(1, 2));
        ChartSeries series = new ChartSeries("cpu", "CPU Usage", ChartSeriesType.LINE, points);
        assertEquals("cpu", series.id());
        assertEquals("CPU Usage", series.name());
        assertEquals(ChartSeriesType.LINE, series.type());
        assertEquals(2, series.points().size());
    }

    @Test
    void emptySeries() {
        ChartSeries series = new ChartSeries("empty", "Empty", ChartSeriesType.AREA, List.of());
        assertTrue(series.points().isEmpty());
    }
}
