package com.youngledo.jmcfx.adapter.jmc;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDataPoint;
import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;

final class JmcResultLimiter {

    static final int MAX_EVENT_ROWS = 5_000;
    static final int MAX_CHART_POINTS_PER_SERIES = 2_000;

    private JmcResultLimiter() {
    }

    static <T> List<T> limitRows(List<T> rows) {
        if (rows.size() <= MAX_EVENT_ROWS) {
            return List.copyOf(rows);
        }
        return List.copyOf(rows.subList(0, MAX_EVENT_ROWS));
    }

    static ChartDefinition limitChart(ChartDefinition definition) {
        if (definition == null || definition.series().isEmpty()) {
            return definition;
        }
        List<ChartSeries> series = definition.series().stream()
                .map(JmcResultLimiter::limitSeries)
                .toList();
        return new ChartDefinition(definition.xLabel(), definition.yLabel(), series);
    }

    private static ChartSeries limitSeries(ChartSeries series) {
        List<ChartDataPoint> points = series.points();
        if (points.size() <= MAX_CHART_POINTS_PER_SERIES) {
            return series;
        }
        return new ChartSeries(series.id(), series.name(), series.type(), sampleEvenly(points));
    }

    private static List<ChartDataPoint> sampleEvenly(List<ChartDataPoint> points) {
        int sourceSize = points.size();
        List<ChartDataPoint> sampled = new ArrayList<>(MAX_CHART_POINTS_PER_SERIES);
        for (int index = 0; index < MAX_CHART_POINTS_PER_SERIES; index++) {
            int sourceIndex = (int) Math.floor(index * (sourceSize - 1.0) / (MAX_CHART_POINTS_PER_SERIES - 1.0));
            sampled.add(points.get(sourceIndex));
        }
        return List.copyOf(sampled);
    }
}
