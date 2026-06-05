package io.github.youngledo.jmcfx.ui.socketio;

import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR Socket I/O tabbed page.
public record SocketIoPageView(
        Label titleLabel,
        Button groupByHostAndPortButton,
        Button groupByHostButton,
        Button groupByPortButton,
        Tab timelineTab,
        Tab durationTab,
        Tab eventLogTab,
        TimelineChart timelineChart,
        TableView<SocketIOHistogram> histogramTable,
        TableView<SocketIOEvent> eventTable) {
}
