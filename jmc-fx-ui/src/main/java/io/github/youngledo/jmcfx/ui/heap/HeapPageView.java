package io.github.youngledo.jmcfx.ui.heap;

import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR Heap data table and timeline page.
public record HeapPageView(
        Label titleLabel,
        Label recordingContextLabel,
        Button clearTimeRangeButton,
        TableView<HeapClassHistogram> table,
        TimelineChart timelineChart) {
}
