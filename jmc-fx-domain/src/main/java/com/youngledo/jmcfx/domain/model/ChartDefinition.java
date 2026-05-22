package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record ChartDefinition(
        String xLabel,
        String yLabel,
        List<ChartSeries> series) {
}
