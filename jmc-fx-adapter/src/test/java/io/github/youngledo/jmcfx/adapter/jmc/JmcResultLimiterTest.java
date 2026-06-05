package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.ChartDataPoint;
import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ChartSeries;
import io.github.youngledo.jmcfx.domain.model.ChartSeriesType;
import io.github.youngledo.jmcfx.domain.model.ChartXAxisType;

class JmcResultLimiterTest {

    @Test
    void limitsEventRowsForNonPagedViews() {
        List<Integer> rows = IntStream.range(0, JmcResultLimiter.MAX_EVENT_ROWS + 100)
                .boxed()
                .toList();

        List<Integer> limited = JmcResultLimiter.limitRows(rows);

        assertEquals(JmcResultLimiter.MAX_EVENT_ROWS, limited.size());
        assertEquals(0, limited.getFirst());
        assertEquals(JmcResultLimiter.MAX_EVENT_ROWS - 1, limited.getLast());
    }

    @Test
    void downsamplesChartSeriesWhilePreservingEndpoints() {
        List<ChartDataPoint> points = IntStream.range(0, JmcResultLimiter.MAX_CHART_POINTS_PER_SERIES * 3)
                .mapToObj(value -> new ChartDataPoint(value, value * 2.0))
                .toList();
        ChartDefinition chart = new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("series", "Series", ChartSeriesType.LINE, points)));

        ChartDefinition limited = JmcResultLimiter.limitChart(chart);
        List<ChartDataPoint> limitedPoints = limited.series().getFirst().points();

        assertEquals(JmcResultLimiter.MAX_CHART_POINTS_PER_SERIES, limitedPoints.size());
        assertEquals(points.getFirst(), limitedPoints.getFirst());
        assertEquals(points.getLast(), limitedPoints.getLast());
        assertTrue(limitedPoints.stream().mapToDouble(ChartDataPoint::x).distinct().count() == limitedPoints.size());
    }

    @Test
    void preservesChartXAxisTypeWhileLimitingSeries() {
        ChartDefinition chart = new ChartDefinition("Time", "Count", ChartXAxisType.EPOCH_SECONDS, List.of(
                new ChartSeries("series", "Series", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 1), new ChartDataPoint(1, 2)))));

        ChartDefinition limited = JmcResultLimiter.limitChart(chart);

        assertEquals(ChartXAxisType.EPOCH_SECONDS, limited.xAxisType());
    }
}
