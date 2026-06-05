package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

public record ChartDefinition(
        String xLabel,
        String yLabel,
        ChartXAxisType xAxisType,
        List<ChartSeries> series) {

    public ChartDefinition(String xLabel, String yLabel, List<ChartSeries> series) {
        this(xLabel, yLabel, ChartXAxisType.NUMBER, series);
    }
}
