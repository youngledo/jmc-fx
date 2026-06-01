package com.youngledo.jmcfx.ui.heap;

import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR Heap data table and timeline page.
public record HeapPageView(
        Label titleLabel,
        TableView<HeapClassHistogram> table,
        TimelineChart timelineChart) {
}
