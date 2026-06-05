package io.github.youngledo.jmcfx.ui.tlab;

import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR TLAB data table and timeline page.
public record TlabPageView(
        Label titleLabel,
        TableView<TlabAllocation> table,
        TimelineChart timelineChart) {
}
