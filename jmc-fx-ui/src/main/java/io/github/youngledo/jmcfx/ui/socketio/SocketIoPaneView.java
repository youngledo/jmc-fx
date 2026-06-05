package io.github.youngledo.jmcfx.ui.socketio;

import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the JFR Socket I/O page.
public final class SocketIoPaneView {

    private final Label titleLabel = new Label();
    private final Label recordingContextLabel = new Label();
    private final HBox groupingBar = new HBox();
    private final Button groupByHostAndPortButton = new Button();
    private final Button groupByHostButton = new Button();
    private final Button groupByPortButton = new Button();
    private final TabPane tabs = new TabPane();
    private final Tab timelineTab = tab();
    private final VBox timelineContainer = new VBox();
    private final TimelineChart timelineChart = new TimelineChart();
    private final Tab durationTab = tab();
    private final TableView<SocketIOHistogram> histogramTable = denseTable();
    private final Tab eventLogTab = tab();
    private final TableView<SocketIOEvent> eventTable = denseTable();

    public SocketIoPaneView(VBox pane) {
        configure(pane);
    }

    public SocketIoPageView view() {
        return new SocketIoPageView(titleLabel, recordingContextLabel, groupByHostAndPortButton, groupByHostButton,
                groupByPortButton, timelineTab, durationTab, eventLogTab,
                timelineChart, histogramTable, eventTable);
    }

    private void configure(VBox pane) {
        groupingBar.setSpacing(8);
        groupingBar.getChildren().setAll(groupByHostAndPortButton, groupByHostButton, groupByPortButton);
        styles(groupingBar, "socketio-grouping-bar");
        timelineContainer.getChildren().setAll(timelineChart);
        tab(timelineTab, timelineContainer);
        tab(durationTab, histogramTable);
        tab(eventLogTab, eventTable);
        tabs.getTabs().setAll(timelineTab, durationTab, eventLogTab);
        configureTablePage(pane, titleLabel, recordingContextLabel, groupingBar, tabs);
    }

    private void configureTablePage(VBox pane, Label title, Node... content) {
        pane.setSpacing(8);
        styles(title, "view-title");
        styles(recordingContextLabel, "detail-panel-meta");
        pane.getChildren().setAll(title);
        pane.getChildren().addAll(content);
        for (Node node : content) {
            if (node instanceof TableView<?> || node instanceof TabPane || node instanceof SplitPane) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }
    }

    private static void tab(Tab tab, Node content) {
        tab.setClosable(false);
        tab.setContent(content);
    }

    private static Tab tab() {
        Tab tab = new Tab();
        tab.setClosable(false);
        return tab;
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
