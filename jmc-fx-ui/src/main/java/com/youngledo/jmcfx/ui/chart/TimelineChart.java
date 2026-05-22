package com.youngledo.jmcfx.ui.chart;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDataPoint;
import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;
import com.youngledo.jmcfx.domain.model.ChartSeriesType;

import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;

/// Reusable XY chart component supporting line, bar, and area series.
public class TimelineChart extends VBox {

    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();

    public TimelineChart() {
        getStyleClass().add("timeline-chart");
    }

    public void setData(ChartDefinition definition) {
        getChildren().clear();
        if (definition == null || definition.series().isEmpty()) {
            return;
        }
        xAxis.setLabel(definition.xLabel());
        yAxis.setLabel(definition.yLabel());
        ChartSeriesType primaryType = definition.series().getFirst().type();
        XYChart<Number, Number> chart = createChart(primaryType);
        for (ChartSeries seriesDef : definition.series()) {
            XYChart.Series<Number, Number> fxSeries = new XYChart.Series<>();
            fxSeries.setName(seriesDef.name());
            for (ChartDataPoint point : seriesDef.points()) {
                fxSeries.getData().add(new XYChart.Data<>(point.x(), point.y()));
            }
            chart.getData().add(fxSeries);
        }
        getChildren().add(chart);
    }

    public int getChartCount() {
        return (int) getChildren().stream().filter(n -> n instanceof XYChart).count();
    }

    private XYChart<Number, Number> createChart(ChartSeriesType type) {
        return switch (type) {
            case LINE -> new LineChart<>(xAxis, yAxis);
            case BAR -> new BarChart<>(xAxis, yAxis);
            case AREA -> new AreaChart<>(xAxis, yAxis);
        };
    }
}
