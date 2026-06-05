package io.github.youngledo.jmcfx.ui.chart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.*;
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
    void appCssContainsSharedDiagnosticChartStyle() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".diagnostic-chart"),
                "app.css must define a shared diagnostic chart style");
        assertTrue(css.contains(".diagnostic-chart .chart-plot-background"),
                "diagnostic charts should own the plot background instead of inheriting JavaFX defaults");
        assertTrue(css.contains(".diagnostic-chart .chart-horizontal-grid-lines"),
                "diagnostic charts should use subtle grid lines for inspection");
        assertTrue(css.contains(".diagnostic-chart .chart-line-symbol"),
                "diagnostic charts should suppress per-point symbols for dense timelines");
        assertTrue(css.contains(".diagnostic-chart .chart-series-line"),
                "diagnostic charts should style series lines consistently");
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

    @Test
    void zoomRangeNarrowsAroundCenter() {
        TimelineChart.AxisRange range = TimelineChart.zoomRange(0, 10, 0.5);

        assertEquals(2.5, range.lowerBound());
        assertEquals(7.5, range.upperBound());
    }

    @Test
    void zoomRangeAtPixelKeepsClickedDataPointUnderPointer() {
        TimelineChart.AxisRange range = TimelineChart.zoomRangeAtPixel(0, 100, 0.5, 300, 400);

        assertEquals(37.5, range.lowerBound());
        assertEquals(87.5, range.upperBound());
    }

    @Test
    void panRangeMovesVisibleWindowByDragDistance() {
        TimelineChart.AxisRange range = TimelineChart.panRange(20, 60, 0, 100, -100, 400);

        assertEquals(30, range.lowerBound());
        assertEquals(70, range.upperBound());
    }

    @Test
    void panRangeClampsToDataRange() {
        TimelineChart.AxisRange left = TimelineChart.panRange(20, 60, 0, 100, 500, 400);
        TimelineChart.AxisRange right = TimelineChart.panRange(20, 60, 0, 100, -500, 400);

        assertEquals(0, left.lowerBound());
        assertEquals(40, left.upperBound());
        assertEquals(60, right.lowerBound());
        assertEquals(100, right.upperBound());
    }

    @Test
    void timelineChartUsesCaptureFiltersForMouseAndScrollGestures() throws IOException {
        String source = timelineChartSource();

        assertTrue(source.contains("addEventFilter(ScrollEvent.SCROLL"),
                "scroll zoom must use event filters so inner chart nodes cannot consume it first");
        assertTrue(source.contains("addEventFilter(ZoomEvent.ZOOM"),
                "macOS trackpad pinch zoom is delivered as ZoomEvent, not ScrollEvent");
        assertTrue(source.contains("event.getZoomFactor()"),
                "trackpad pinch zoom must use the gesture zoom factor");
        assertTrue(source.contains("zoomXAxisAtPixel(1 / event.getZoomFactor(), event.getX(), chart.getWidth())"),
                "trackpad pinch zoom should use the gesture position as the zoom anchor");
        assertTrue(source.contains("panXAxis(event.getDeltaX()"),
                "trackpad horizontal scroll should pan the visible time window");
        assertTrue(source.contains("zoomXAxisAtPixel(ZOOM_FACTOR, event.getX(), chart.getWidth())"),
                "double-click should zoom into the clicked point instead of resetting the chart");
        assertFalse(source.contains("resetZoom();"),
                "double-click reset conflicts with common chart zoom interaction");
        assertTrue(source.contains("addEventFilter(MouseEvent.MOUSE_DRAGGED"),
                "drag panning must use event filters so inner chart nodes cannot consume it first");
        assertTrue(source.contains("event.isPrimaryButtonDown()"),
                "drag panning must not use MouseEvent#getButton(), which is NONE during drag");
    }

    @Test
    void timelineChartRequestsAvailableVerticalSpace() throws IOException {
        String source = timelineChartSource();

        assertTrue(source.contains("VBox.setVgrow(chart, Priority.ALWAYS"));
        assertTrue(source.contains("chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE"));
    }

    @Test
    void timelineChartUsesDiagnosticChartTreatment() throws IOException {
        String source = timelineChartSource();

        assertTrue(source.contains("getStyleClass().addAll(\"timeline-chart\", \"diagnostic-chart\")"));
        assertTrue(source.contains("configureDiagnosticChart(chart);"));
        assertTrue(source.contains("lineChart.setCreateSymbols(false);"));
        assertTrue(source.contains("areaChart.setCreateSymbols(false);"));
    }

    @Test
    void barChartsUseCategoryAxisBecauseJavaFxBarChartRejectsTwoNumberAxes() throws IOException {
        String source = timelineChartSource();

        assertTrue(source.contains("CategoryAxis"),
                "JavaFX BarChart requires one CategoryAxis and one NumberAxis");
        assertFalse(source.contains("new BarChart<>(xAxis, yAxis)"),
                "BarChart must not be constructed with two NumberAxis instances");
    }

    @Test
    void barChartsUseDiagnosticChartTreatment() throws IOException {
        String source = timelineChartSource();
        String css = appCss();

        assertTrue(source.contains("configureDiagnosticChart(chart);"));
        assertTrue(source.contains("chart.setCategoryGap(6);"));
        assertTrue(source.contains("chart.setBarGap(2);"));
        assertTrue(css.contains(".diagnostic-chart .chart-bar"));
    }

    @Test
    void dataRangeUsesAllSeriesPoints() {
        ChartDefinition definition = new ChartDefinition("Time", "Value", List.of(
                new ChartSeries("cpu", "CPU", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(3, 10), new ChartDataPoint(7, 20))),
                new ChartSeries("mem", "Memory", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(1, 5), new ChartDataPoint(11, 15)))));

        TimelineChart.AxisRange range = TimelineChart.dataRange(definition);

        assertEquals(1, range.lowerBound());
        assertEquals(11, range.upperBound());
    }

    @Test
    void dataRangePadsSinglePointSeries() {
        ChartDefinition definition = new ChartDefinition("Time", "Value", List.of(
                new ChartSeries("cpu", "CPU", ChartSeriesType.LINE, List.of(new ChartDataPoint(5, 10)))));

        TimelineChart.AxisRange range = TimelineChart.dataRange(definition);

        assertEquals(4.5, range.lowerBound());
        assertEquals(5.5, range.upperBound());
    }

    @Test
    void renderableDefinitionDownsamplesLargeSeriesBeforeJavaFxNodesAreCreated() {
        List<ChartDataPoint> points = java.util.stream.IntStream
                .range(0, TimelineChart.MAX_RENDERED_POINTS_PER_SERIES * 3)
                .mapToObj(value -> new ChartDataPoint(value, value))
                .toList();
        ChartDefinition definition = new ChartDefinition("Time", "Value", List.of(
                new ChartSeries("large", "Large", ChartSeriesType.LINE, points)));

        ChartDefinition renderable = TimelineChart.renderableDefinition(definition);
        List<ChartDataPoint> renderedPoints = renderable.series().getFirst().points();

        assertEquals(TimelineChart.MAX_RENDERED_POINTS_PER_SERIES, renderedPoints.size());
        assertEquals(points.getFirst(), renderedPoints.getFirst());
        assertEquals(points.getLast(), renderedPoints.getLast());
    }

    @Test
    void epochMillisAxisTicksFormatAsTimestamps() {
        assertEquals("1970-01-01 08:00:00.000",
                TimelineChart.formatXAxisTick(ChartXAxisType.EPOCH_MILLIS, 0, java.time.ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void epochSecondsAxisTicksFormatAsTimestamps() {
        assertEquals("1970-01-01 08:00:01.000",
                TimelineChart.formatXAxisTick(ChartXAxisType.EPOCH_SECONDS, 1, java.time.ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void numberAxisTicksKeepNumericLabelsEvenWhenLabelSaysTime() {
        assertEquals("1",
                TimelineChart.formatXAxisTick(ChartXAxisType.NUMBER, 1, java.time.ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void nonTimeAxisTicksKeepNumericLabels() {
        assertEquals("42.5",
                TimelineChart.formatXAxisTick(ChartXAxisType.NUMBER, 42.5, java.time.ZoneId.of("Asia/Shanghai")));
    }

    private static String appCss() throws IOException {
        try (InputStream stream = TimelineChartTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String timelineChartSource() throws IOException {
        return java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/io/github/youngledo/jmcfx/ui/chart/TimelineChart.java"));
    }
}
