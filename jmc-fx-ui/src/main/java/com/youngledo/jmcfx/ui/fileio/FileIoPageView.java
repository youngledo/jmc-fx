package com.youngledo.jmcfx.ui.fileio;

import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR File I/O tabbed page.
public record FileIoPageView(
        Label titleLabel,
        Tab timelineTab,
        Tab durationTab,
        Tab eventLogTab,
        TimelineChart timelineChart,
        TableView<FileIOHistogram> histogramTable,
        TableView<FileIOEvent> eventTable) {
}
