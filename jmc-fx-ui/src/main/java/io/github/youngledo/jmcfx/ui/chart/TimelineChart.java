package io.github.youngledo.jmcfx.ui.chart;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDataPoint;
import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ChartSeries;
import io.github.youngledo.jmcfx.domain.model.ChartSeriesType;
import io.github.youngledo.jmcfx.domain.model.ChartXAxisType;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

/// Reusable XY chart component supporting line, bar, and area series.
public class TimelineChart extends VBox {

    private static final double ZOOM_FACTOR = 0.85;
    private static final double MIN_SELECTION_PIXELS = 4.0;
    private static final int TARGET_MAJOR_TICK_COUNT = 8;
    static final int MAX_RENDERED_POINTS_PER_SERIES = 2_000;

    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final ReadOnlyObjectWrapper<AxisRange> userSelectedRange = new ReadOnlyObjectWrapper<>();
    private final Pane selectionOverlay = new Pane();
    private final Rectangle leftMutedRegion = selectionRegion("timeline-selection-muted");
    private final Rectangle selectedRegion = selectionRegion("timeline-selection-band");
    private final Rectangle rightMutedRegion = selectionRegion("timeline-selection-muted");
    private double initialLowerBound;
    private double initialUpperBound;
    private double selectionStartX = Double.NaN;

    public TimelineChart() {
        getStyleClass().addAll("timeline-chart", "diagnostic-chart");
        configureSelectionOverlay();
    }

    public void setData(ChartDefinition definition) {
        getChildren().clear();
        clearUserSelection();
        if (definition == null || definition.series().isEmpty()) {
            return;
        }
        ChartDefinition renderableDefinition = renderableDefinition(definition);
        xAxis.setLabel(renderableDefinition.xLabel());
        xAxis.setTickLabelFormatter(new XAxisTickFormatter(renderableDefinition.xAxisType(), ZoneId.systemDefault()));
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
        configureDiagnosticChart(chart);
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
        StackPane chartLayer = new StackPane(chart, selectionOverlay);
        VBox.setVgrow(chartLayer, Priority.ALWAYS);
        chartLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getChildren().add(chartLayer);
    }

    static ChartDefinition renderableDefinition(ChartDefinition definition) {
        if (definition == null || definition.series().isEmpty()) {
            return definition;
        }
        List<ChartSeries> series = definition.series().stream()
                .map(TimelineChart::renderableSeries)
                .toList();
        return new ChartDefinition(definition.xLabel(), definition.yLabel(), definition.xAxisType(), series);
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
        return countCharts(this);
    }

    public double xAxisLowerBound() {
        return xAxis.getLowerBound();
    }

    public double xAxisUpperBound() {
        return xAxis.getUpperBound();
    }

    public ReadOnlyObjectProperty<AxisRange> userSelectedRangeProperty() {
        return userSelectedRange.getReadOnlyProperty();
    }

    public void setUserSelection(AxisRange range) {
        userSelectedRange.set(range);
        refreshSelectionOverlay();
    }

    public void clearUserSelection() {
        userSelectedRange.set(null);
        selectionStartX = Double.NaN;
        clearSelectionOverlay();
    }

    private XYChart<Number, Number> createChart(ChartSeriesType type) {
        return switch (type) {
            case LINE -> {
                LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
                lineChart.setCreateSymbols(false);
                yield lineChart;
            }
            case BAR -> throw new IllegalArgumentException("Bar charts require a CategoryAxis");
            case AREA -> {
                AreaChart<Number, Number> areaChart = new AreaChart<>(xAxis, yAxis);
                areaChart.setCreateSymbols(false);
                yield areaChart;
            }
        };
    }

    private static int countCharts(javafx.scene.Node node) {
        if (node instanceof XYChart<?, ?>) {
            return 1;
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream()
                    .mapToInt(TimelineChart::countCharts)
                    .sum();
        }
        return 0;
    }

    private static void configureDiagnosticChart(XYChart<?, ?> chart) {
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);
    }

    private XYChart<String, Number> createBarChart(ChartDefinition definition) {
        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis valueAxis = new NumberAxis();
        categoryAxis.setLabel(definition.xLabel());
        valueAxis.setLabel(definition.yLabel());
        BarChart<String, Number> chart = new BarChart<>(categoryAxis, valueAxis);
        configureDiagnosticChart(chart);
        chart.setCategoryGap(6);
        chart.setBarGap(2);
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
                selectionStartX = clampPixel(event.getX(), chart.getWidth());
                renderSelectionOverlay(selectionStartX, selectionStartX, chart.getWidth(), chart.getHeight());
                event.consume();
            }
        });
        chart.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.isPrimaryButtonDown() && !Double.isNaN(selectionStartX)) {
                renderSelectionOverlay(selectionStartX, clampPixel(event.getX(), chart.getWidth()),
                        chart.getWidth(), chart.getHeight());
                event.consume();
            }
        });
        chart.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectXAxisRange(selectionStartX, event.getX(), chart.getWidth());
                selectionStartX = Double.NaN;
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
        setAxisBoundsAndRefresh(range.lowerBound(), range.upperBound());
    }

    void resetZoom() {
        setAxisBoundsAndRefresh(initialLowerBound, initialUpperBound);
    }

    void zoomXAxisAtPixel(double factor, double pixelX, double pixelWidth) {
        if (factor <= 0 || pixelWidth <= 0 || xAxis.getLowerBound() >= xAxis.getUpperBound()) {
            return;
        }
        xAxis.setAutoRanging(false);
        AxisRange range = zoomRangeAtPixel(xAxis.getLowerBound(), xAxis.getUpperBound(), factor, pixelX, pixelWidth);
        setAxisBoundsAndRefresh(range.lowerBound(), range.upperBound());
    }

    void panXAxis(double dragDeltaX, double pixelWidth) {
        AxisRange range = panRange(xAxis.getLowerBound(), xAxis.getUpperBound(),
                initialLowerBound, initialUpperBound, dragDeltaX, pixelWidth);
        setAxisBoundsAndRefresh(range.lowerBound(), range.upperBound());
    }

    void selectXAxisRange(double startPixel, double endPixel, double pixelWidth) {
        if (Double.isNaN(startPixel) || pixelWidth <= 0) {
            clearSelectionOverlay();
            return;
        }
        double clampedStart = clampPixel(startPixel, pixelWidth);
        double clampedEnd = clampPixel(endPixel, pixelWidth);
        if (Math.abs(clampedEnd - clampedStart) < MIN_SELECTION_PIXELS) {
            clearUserSelection();
            return;
        }
        AxisRange range = rangeFromPixels(xAxis.getLowerBound(), xAxis.getUpperBound(),
                clampedStart, clampedEnd, pixelWidth);
        setUserSelection(range);
    }

    private static void setAxisBounds(NumberAxis axis, double lowerBound, double upperBound) {
        axis.setAutoRanging(false);
        axis.setLowerBound(lowerBound);
        axis.setUpperBound(upperBound);
        axis.setTickUnit(tickUnit(lowerBound, upperBound));
        axis.setMinorTickVisible(false);
    }

    private void setAxisBoundsAndRefresh(double lowerBound, double upperBound) {
        setAxisBounds(xAxis, lowerBound, upperBound);
        refreshSelectionOverlay();
    }

    static AxisRange rangeFromPixels(double lowerBound, double upperBound,
            double startPixel, double endPixel, double pixelWidth) {
        double start = valueAtPixel(lowerBound, upperBound, startPixel, pixelWidth);
        double end = valueAtPixel(lowerBound, upperBound, endPixel, pixelWidth);
        return new AxisRange(Math.min(start, end), Math.max(start, end));
    }

    static double valueAtPixel(double lowerBound, double upperBound, double pixelX, double pixelWidth) {
        if (pixelWidth <= 0 || lowerBound >= upperBound) {
            return lowerBound;
        }
        double ratio = Math.clamp(pixelX / pixelWidth, 0, 1);
        return lowerBound + (upperBound - lowerBound) * ratio;
    }

    static double pixelForValue(double lowerBound, double upperBound, double value, double pixelWidth) {
        if (pixelWidth <= 0 || lowerBound >= upperBound) {
            return 0;
        }
        double ratio = (value - lowerBound) / (upperBound - lowerBound);
        return Math.clamp(ratio, 0, 1) * pixelWidth;
    }

    private static double clampPixel(double pixel, double pixelWidth) {
        if (!Double.isFinite(pixel) || pixelWidth <= 0) {
            return 0;
        }
        return Math.clamp(pixel, 0, pixelWidth);
    }

    private void configureSelectionOverlay() {
        selectionOverlay.getStyleClass().add("timeline-selection-overlay");
        selectionOverlay.setMouseTransparent(true);
        selectionOverlay.getChildren().setAll(leftMutedRegion, selectedRegion, rightMutedRegion);
        selectionOverlay.widthProperty().addListener((observable, oldValue, newValue) -> refreshSelectionOverlay());
        selectionOverlay.heightProperty().addListener((observable, oldValue, newValue) -> refreshSelectionOverlay());
        clearSelectionOverlay();
    }

    private static Rectangle selectionRegion(String styleClass) {
        Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add(styleClass);
        rectangle.setManaged(false);
        rectangle.setVisible(false);
        return rectangle;
    }

    private void refreshSelectionOverlay() {
        AxisRange range = userSelectedRange.get();
        if (range == null || selectionOverlay.getWidth() <= 0) {
            clearSelectionOverlay();
            return;
        }
        double startPixel = pixelForValue(xAxis.getLowerBound(), xAxis.getUpperBound(),
                range.lowerBound(), selectionOverlay.getWidth());
        double endPixel = pixelForValue(xAxis.getLowerBound(), xAxis.getUpperBound(),
                range.upperBound(), selectionOverlay.getWidth());
        renderSelectionOverlay(startPixel, endPixel, selectionOverlay.getWidth(), selectionOverlay.getHeight());
    }

    private void renderSelectionOverlay(double startPixel, double endPixel, double width, double height) {
        if (width <= 0 || height <= 0) {
            clearSelectionOverlay();
            return;
        }
        double start = clampPixel(Math.min(startPixel, endPixel), width);
        double end = clampPixel(Math.max(startPixel, endPixel), width);
        layoutRegion(leftMutedRegion, 0, start, height);
        layoutRegion(selectedRegion, start, end - start, height);
        layoutRegion(rightMutedRegion, end, width - end, height);
    }

    private static void layoutRegion(Rectangle region, double x, double width, double height) {
        region.setVisible(width > 0 && height > 0);
        region.setX(x);
        region.setY(0);
        region.setWidth(Math.max(0, width));
        region.setHeight(Math.max(0, height));
    }

    private void clearSelectionOverlay() {
        leftMutedRegion.setVisible(false);
        selectedRegion.setVisible(false);
        rightMutedRegion.setVisible(false);
    }

    static double tickUnit(double lowerBound, double upperBound) {
        double range = upperBound - lowerBound;
        if (!Double.isFinite(range) || range <= 0) {
            return 1;
        }
        double rawUnit = range / TARGET_MAJOR_TICK_COUNT;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawUnit)));
        double normalized = rawUnit / magnitude;
        double niceNormalized = normalized <= 1 ? 1
                : normalized <= 2 ? 2
                : normalized <= 5 ? 5
                : 10;
        return niceNormalized * magnitude;
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

    static String formatXAxisTick(ChartXAxisType axisType, double value, ZoneId zoneId) {
        return switch (axisType == null ? ChartXAxisType.NUMBER : axisType) {
            case EPOCH_MILLIS -> DisplayFormats.formatTimestamp(Instant.ofEpochMilli(Math.round(value)), zoneId);
            case EPOCH_SECONDS -> DisplayFormats.formatTimestamp(Instant.ofEpochMilli(Math.round(value * 1000)), zoneId);
            case NUMBER -> formatNumericTick(value);
        };
    }

    private static String formatNumericTick(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static final class XAxisTickFormatter extends StringConverter<Number> {

        private final ChartXAxisType axisType;
        private final ZoneId zoneId;

        private XAxisTickFormatter(ChartXAxisType axisType, ZoneId zoneId) {
            this.axisType = axisType;
            this.zoneId = zoneId;
        }

        @Override
        public String toString(Number value) {
            if (value == null) {
                return "";
            }
            return formatXAxisTick(axisType, value.doubleValue(), zoneId);
        }

        @Override
        public Number fromString(String string) {
            return 0;
        }
    }

    public record AxisRange(double lowerBound, double upperBound) {
    }
}
