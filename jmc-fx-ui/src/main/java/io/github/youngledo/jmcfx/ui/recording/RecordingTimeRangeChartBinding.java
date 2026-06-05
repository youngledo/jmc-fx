package io.github.youngledo.jmcfx.ui.recording;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ChartXAxisType;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;

/// Synchronizes one timeline chart's brush selection with a recording workspace time range.
public final class RecordingTimeRangeChartBinding implements AutoCloseable {

    private final TimelineChart chart;
    private final ObjectProperty<RecordingTimeRange> timeRange;
    private final ChangeListener<TimelineChart.AxisRange> chartSelectionListener;
    private final ChangeListener<RecordingTimeRange> timeRangeListener;
    private ChartXAxisType xAxisType = ChartXAxisType.EPOCH_MILLIS;
    private boolean applyingTimeRange;

    public RecordingTimeRangeChartBinding(TimelineChart chart, ObjectProperty<RecordingTimeRange> timeRange) {
        this.chart = Objects.requireNonNull(chart, "chart");
        this.timeRange = timeRange;
        chartSelectionListener = (observable, oldValue, newValue) -> updateTimeRange(newValue);
        timeRangeListener = (observable, oldValue, newValue) -> applyTimeRange(newValue);
        chart.userSelectedRangeProperty().addListener(chartSelectionListener);
        if (timeRange != null) {
            timeRange.addListener(timeRangeListener);
            applyTimeRange(timeRange.get());
        } else {
            chart.clearUserSelection();
        }
    }

    public void setData(ChartDefinition definition) {
        applyingTimeRange = true;
        try {
            xAxisType = definition == null ? ChartXAxisType.EPOCH_MILLIS : definition.xAxisType();
            chart.setData(definition);
        } finally {
            applyingTimeRange = false;
        }
        applyTimeRange(timeRange == null ? null : timeRange.get());
    }

    @Override
    public void close() {
        chart.userSelectedRangeProperty().removeListener(chartSelectionListener);
        if (timeRange != null) {
            timeRange.removeListener(timeRangeListener);
        }
        chart.clearUserSelection();
    }

    private void updateTimeRange(TimelineChart.AxisRange range) {
        if (applyingTimeRange || timeRange == null) {
            return;
        }
        if (range == null) {
            timeRange.set(null);
            return;
        }
        timeRange.set(RecordingTimeRange.fromBounds(
                toEpochMillis(range.lowerBound()),
                toEpochMillis(range.upperBound())));
    }

    private void applyTimeRange(RecordingTimeRange range) {
        applyingTimeRange = true;
        try {
            if (range == null) {
                chart.clearUserSelection();
                return;
            }
            chart.setUserSelection(new TimelineChart.AxisRange(
                    fromEpochMillis(range.startEpochMillis()),
                    fromEpochMillis(range.endEpochMillis())));
        } finally {
            applyingTimeRange = false;
        }
    }

    private double fromEpochMillis(long epochMillis) {
        return switch (xAxisType) {
            case EPOCH_SECONDS -> epochMillis / 1_000.0;
            case EPOCH_MILLIS, NUMBER -> epochMillis;
        };
    }

    private double toEpochMillis(double axisValue) {
        return switch (xAxisType) {
            case EPOCH_SECONDS -> axisValue * 1_000.0;
            case EPOCH_MILLIS, NUMBER -> axisValue;
        };
    }
}
