package com.youngledo.jmcfx.ui.chart;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDataPoint;
import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;
import com.youngledo.jmcfx.domain.model.ChartSeriesType;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.util.StringConverter;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Reusable XY chart component supporting line, bar, and area series.
public class TimelineChart extends VBox {

    private static final double ZOOM_FACTOR = 0.85;
    static final int MAX_RENDERED_POINTS_PER_SERIES = 2_000;

    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private double initialLowerBound;
    private double initialUpperBound;
    private double dragStartX = Double.NaN;
    private double dragStartLowerBound;
    private double dragStartUpperBound;

    public TimelineChart() {
        getStyleClass().add("timeline-chart");
    }

    public void setData(ChartDefinition definition) {
        getChildren().clear();
        if (definition == null || definition.series().isEmpty()) {
            return;
        }
        ChartDefinition renderableDefinition = renderableDefinition(definition);
        xAxis.setLabel(renderableDefinition.xLabel());
        xAxis.setTickLabelFormatter(new XAxisTickFormatter(renderableDefinition.xLabel(), ZoneId.systemDefault()));
        yAxis.setLabel(renderableDefinition.yLabel());
        AxisRange initialRange = dataRange(renderableDefinition);
        ChartSeriesType primaryType = renderableDefinition.series().getFirst().type();
        if (primaryType == ChartSeriesType.BAR) {
            XYChart<String, Number> chart = createBarChart(renderableDefinition);
            VBox.setVgrow(chart, Priority.ALWAYS);
            chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            getChildren().add(chart);
            return;
        }
        XYChart<Number, Number> chart = createChart(primaryType);
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        for (ChartSeries seriesDef : renderableDefinition.series()) {
            XYChart.Series<Number, Number> fxSeries = new XYChart.Series<>();
            fxSeries.setName(seriesDef.name());
            for (ChartDataPoint point : seriesDef.points()) {
                fxSeries.getData().add(new XYChart.Data<>(point.x(), point.y()));
            }
            chart.getData().add(fxSeries);
        }
        configureZoom(chart, initialRange);
        getChildren().add(chart);
    }

    static ChartDefinition renderableDefinition(ChartDefinition definition) {
        if (definition == null || definition.series().isEmpty()) {
            return definition;
        }
        List<ChartSeries> series = definition.series().stream()
                .map(TimelineChart::renderableSeries)
                .toList();
        return new ChartDefinition(definition.xLabel(), definition.yLabel(), series);
    }

    private static ChartSeries renderableSeries(ChartSeries series) {
        if (series.points().size() <= MAX_RENDERED_POINTS_PER_SERIES) {
            return series;
        }
        return new ChartSeries(series.id(), series.name(), series.type(), sampleEvenly(series.points()));
    }

    private static List<ChartDataPoint> sampleEvenly(List<ChartDataPoint> points) {
        int sourceSize = points.size();
        List<ChartDataPoint> sampled = new ArrayList<>(MAX_RENDERED_POINTS_PER_SERIES);
        for (int index = 0; index < MAX_RENDERED_POINTS_PER_SERIES; index++) {
            int sourceIndex = (int) Math.floor(index * (sourceSize - 1.0) / (MAX_RENDERED_POINTS_PER_SERIES - 1.0));
            sampled.add(points.get(sourceIndex));
        }
        return List.copyOf(sampled);
    }

    public int getChartCount() {
        return (int) getChildren().stream().filter(n -> n instanceof XYChart).count();
    }

    public double xAxisLowerBound() {
        return xAxis.getLowerBound();
    }

    public double xAxisUpperBound() {
        return xAxis.getUpperBound();
    }

    private XYChart<Number, Number> createChart(ChartSeriesType type) {
        return switch (type) {
            case LINE -> new LineChart<>(xAxis, yAxis);
            case BAR -> throw new IllegalArgumentException("Bar charts require a CategoryAxis");
            case AREA -> new AreaChart<>(xAxis, yAxis);
        };
    }

    private XYChart<String, Number> createBarChart(ChartDefinition definition) {
        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis valueAxis = new NumberAxis();
        categoryAxis.setLabel(definition.xLabel());
        valueAxis.setLabel(definition.yLabel());
        BarChart<String, Number> chart = new BarChart<>(categoryAxis, valueAxis);
        chart.setLegendVisible(definition.series().size() > 1);
        for (ChartSeries seriesDef : definition.series()) {
            XYChart.Series<String, Number> fxSeries = new XYChart.Series<>();
            fxSeries.setName(seriesDef.name());
            for (ChartDataPoint point : seriesDef.points()) {
                fxSeries.getData().add(new XYChart.Data<>(Double.toString(point.x()), point.y()));
            }
            chart.getData().add(fxSeries);
        }
        return chart;
    }

    private void configureZoom(XYChart<Number, Number> chart, AxisRange initialRange) {
        initialLowerBound = initialRange.lowerBound();
        initialUpperBound = initialRange.upperBound();
        setAxisBounds(xAxis, initialLowerBound, initialUpperBound);
        chart.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY())) {
                panXAxis(event.getDeltaX(), chart.getWidth());
            } else {
                zoomXAxis(event.getDeltaY() > 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR);
            }
            event.consume();
        });
        chart.addEventFilter(ZoomEvent.ZOOM, event -> {
            zoomXAxisAtPixel(1 / event.getZoomFactor(), event.getX(), chart.getWidth());
            event.consume();
        });
        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                zoomXAxisAtPixel(ZOOM_FACTOR, event.getX(), chart.getWidth());
                event.consume();
            }
        });
        chart.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                dragStartX = event.getX();
                dragStartLowerBound = xAxis.getLowerBound();
                dragStartUpperBound = xAxis.getUpperBound();
                event.consume();
            }
        });
        chart.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.isPrimaryButtonDown() && !Double.isNaN(dragStartX)) {
                panXAxisFromDragStart(event.getX() - dragStartX, chart.getWidth());
                event.consume();
            }
        });
        chart.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                dragStartX = Double.NaN;
                event.consume();
            }
        });
    }

    void zoomXAxis(double factor) {
        if (factor <= 0 || xAxis.getLowerBound() >= xAxis.getUpperBound()) {
            return;
        }
        xAxis.setAutoRanging(false);
        AxisRange range = zoomRange(xAxis.getLowerBound(), xAxis.getUpperBound(), factor);
        xAxis.setLowerBound(range.lowerBound());
        xAxis.setUpperBound(range.upperBound());
    }

    void resetZoom() {
        setAxisBounds(xAxis, initialLowerBound, initialUpperBound);
    }

    void zoomXAxisAtPixel(double factor, double pixelX, double pixelWidth) {
        if (factor <= 0 || pixelWidth <= 0 || xAxis.getLowerBound() >= xAxis.getUpperBound()) {
            return;
        }
        xAxis.setAutoRanging(false);
        AxisRange range = zoomRangeAtPixel(xAxis.getLowerBound(), xAxis.getUpperBound(), factor, pixelX, pixelWidth);
        xAxis.setLowerBound(range.lowerBound());
        xAxis.setUpperBound(range.upperBound());
    }

    void panXAxis(double dragDeltaX, double pixelWidth) {
        AxisRange range = panRange(xAxis.getLowerBound(), xAxis.getUpperBound(),
                initialLowerBound, initialUpperBound, dragDeltaX, pixelWidth);
        xAxis.setLowerBound(range.lowerBound());
        xAxis.setUpperBound(range.upperBound());
    }

    void panXAxisFromDragStart(double dragDeltaX, double pixelWidth) {
        AxisRange range = panRange(dragStartLowerBound, dragStartUpperBound,
                initialLowerBound, initialUpperBound, dragDeltaX, pixelWidth);
        xAxis.setLowerBound(range.lowerBound());
        xAxis.setUpperBound(range.upperBound());
    }

    private static void setAxisBounds(ValueAxis<Number> axis, double lowerBound, double upperBound) {
        axis.setAutoRanging(false);
        axis.setLowerBound(lowerBound);
        axis.setUpperBound(upperBound);
    }

    static AxisRange zoomRange(double lowerBound, double upperBound, double factor) {
        if (factor <= 0 || lowerBound >= upperBound) {
            return new AxisRange(lowerBound, upperBound);
        }
        double center = (lowerBound + upperBound) / 2.0;
        double halfRange = (upperBound - lowerBound) * factor / 2.0;
        return new AxisRange(center - halfRange, center + halfRange);
    }

    static AxisRange zoomRangeAtPixel(double lowerBound, double upperBound, double factor,
            double pixelX, double pixelWidth) {
        if (factor <= 0 || pixelWidth <= 0 || lowerBound >= upperBound) {
            return new AxisRange(lowerBound, upperBound);
        }
        double visibleRange = upperBound - lowerBound;
        double pointerRatio = Math.clamp(pixelX / pixelWidth, 0, 1);
        double anchorValue = lowerBound + visibleRange * pointerRatio;
        double nextRange = visibleRange * factor;
        double nextLower = anchorValue - nextRange * pointerRatio;
        double nextUpper = nextLower + nextRange;
        return new AxisRange(nextLower, nextUpper);
    }

    static AxisRange panRange(double lowerBound, double upperBound, double minBound, double maxBound,
            double dragDeltaX, double pixelWidth) {
        if (pixelWidth <= 0 || lowerBound >= upperBound || minBound >= maxBound) {
            return new AxisRange(lowerBound, upperBound);
        }
        double visibleRange = upperBound - lowerBound;
        double dataRange = maxBound - minBound;
        if (visibleRange >= dataRange) {
            return new AxisRange(minBound, maxBound);
        }

        double shift = -(dragDeltaX / pixelWidth) * visibleRange;
        double nextLower = lowerBound + shift;
        double nextUpper = upperBound + shift;
        if (nextLower < minBound) {
            nextLower = minBound;
            nextUpper = minBound + visibleRange;
        } else if (nextUpper > maxBound) {
            nextUpper = maxBound;
            nextLower = maxBound - visibleRange;
        }
        return new AxisRange(nextLower, nextUpper);
    }

    static AxisRange dataRange(ChartDefinition definition) {
        List<Double> values = definition.series().stream()
                .flatMap(series -> series.points().stream())
                .map(ChartDataPoint::x)
                .toList();
        if (values.isEmpty()) {
            return new AxisRange(0, 1);
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (min == max) {
            return new AxisRange(min - 0.5, max + 0.5);
        }
        return new AxisRange(min, max);
    }

    static String formatXAxisTick(String axisLabel, double value, ZoneId zoneId) {
        if (axisLabel != null && axisLabel.equalsIgnoreCase("Time")) {
            return DisplayFormats.formatTimestamp(Instant.ofEpochMilli(Math.round(value)), zoneId);
        }
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static final class XAxisTickFormatter extends StringConverter<Number> {

        private final String axisLabel;
        private final ZoneId zoneId;

        private XAxisTickFormatter(String axisLabel, ZoneId zoneId) {
            this.axisLabel = axisLabel;
            this.zoneId = zoneId;
        }

        @Override
        public String toString(Number value) {
            if (value == null) {
                return "";
            }
            return formatXAxisTick(axisLabel, value.doubleValue(), zoneId);
        }

        @Override
        public Number fromString(String string) {
            return 0;
        }
    }

    record AxisRange(double lowerBound, double upperBound) {
    }
}
