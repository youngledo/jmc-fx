package io.github.youngledo.jmcfx.ui.tlab;

import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR TLAB data table and timeline page.
public record TlabPageView(
        Label titleLabel,
        Label recordingContextLabel,
        Button clearTimeRangeButton,
        TableView<TlabAllocation> table,
        TimelineChart timelineChart) {
}
