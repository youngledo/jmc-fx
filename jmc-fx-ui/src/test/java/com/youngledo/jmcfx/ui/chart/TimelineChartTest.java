package com.youngledo.jmcfx.ui.chart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.youngledo.jmcfx.domain.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Tests TimelineChart structural and CSS properties without JavaFX toolkit initialization.
class TimelineChartTest {

    @Test
    void appCssContainsTimelineChartStyle() throws IOException {
        String css = appCss();
        assertTrue(css.contains(".timeline-chart"),
                "app.css must define .timeline-chart style for TimelineChart");
        assertTrue(css.contains(".chart-plot-background"),
                "app.css must style chart-plot-background for TimelineChart");
    }

    @Test
    void chartDomainRecordsSupportAllSeriesTypes() {
        ChartSeries lineSeries = new ChartSeries("cpu", "CPU", ChartSeriesType.LINE,
                List.of(new ChartDataPoint(0, 10), new ChartDataPoint(1, 20)));
        assertEquals(ChartSeriesType.LINE, lineSeries.type());
        assertEquals(2, lineSeries.points().size());
        assertEquals(10.0, lineSeries.points().getFirst().y());

        ChartSeries barSeries = new ChartSeries("mem", "Memory", ChartSeriesType.BAR,
                List.of(new ChartDataPoint(0, 50)));
        assertEquals(ChartSeriesType.BAR, barSeries.type());

        ChartSeries areaSeries = new ChartSeries("gc", "GC", ChartSeriesType.AREA,
                List.of(new ChartDataPoint(0, 5)));
        assertEquals(ChartSeriesType.AREA, areaSeries.type());
    }

    @Test
    void chartDefinitionAggregatesSeries() {
        ChartSeries series = new ChartSeries("cpu", "CPU", ChartSeriesType.LINE,
                List.of(new ChartDataPoint(0, 10)));
        ChartDefinition definition = new ChartDefinition("Time", "Value", List.of(series));
        assertEquals("Time", definition.xLabel());
        assertEquals("Value", definition.yLabel());
        assertEquals(1, definition.series().size());
    }

    @Test
    void chartSeriesTypeCoversAllVariants() {
        assertEquals(3, ChartSeriesType.values().length);
        assertNotNull(ChartSeriesType.valueOf("LINE"));
        assertNotNull(ChartSeriesType.valueOf("BAR"));
        assertNotNull(ChartSeriesType.valueOf("AREA"));
    }

    private static String appCss() throws IOException {
        try (InputStream stream = TimelineChartTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
