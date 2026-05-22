package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record ChartSeries(
        String id,
        String name,
        ChartSeriesType type,
        List<ChartDataPoint> points) {
}
