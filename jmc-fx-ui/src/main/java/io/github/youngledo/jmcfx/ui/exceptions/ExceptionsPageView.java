package io.github.youngledo.jmcfx.ui.exceptions;

import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR Exceptions data table and timeline page.
public record ExceptionsPageView(
        Label titleLabel,
        Label recordingContextLabel,
        Button groupByClassButton,
        Button groupByMessageButton,
        Button groupByClassAndMessageButton,
        TableView<ExceptionSummary> table,
        TimelineChart timelineChart) {
}
