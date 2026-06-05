package io.github.youngledo.jmcfx.ui.fileio;

import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR File I/O tabbed page.
public record FileIoPageView(
        Label titleLabel,
        Label recordingContextLabel,
        Button clearTimeRangeButton,
        Tab timelineTab,
        Tab durationTab,
        Tab eventLogTab,
        TimelineChart timelineChart,
        TableView<FileIOHistogram> histogramTable,
        TableView<FileIOEvent> eventTable) {
}
