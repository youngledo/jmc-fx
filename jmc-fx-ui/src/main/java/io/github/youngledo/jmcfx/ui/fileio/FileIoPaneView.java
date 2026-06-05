package io.github.youngledo.jmcfx.ui.fileio;

import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
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

/// Code-first view for the JFR File I/O page.
public final class FileIoPaneView {

    private final Label titleLabel = new Label();
    private final Label recordingContextLabel = new Label();
    private final Button clearTimeRangeButton = new Button();
    private final HBox recordingContextBar = new HBox();
    private final TabPane tabs = new TabPane();
    private final Tab timelineTab = tab();
    private final VBox timelineContainer = new VBox();
    private final TimelineChart timelineChart = new TimelineChart();
    private final Tab durationTab = tab();
    private final TableView<FileIOHistogram> histogramTable = denseTable();
    private final Tab eventLogTab = tab();
    private final TableView<FileIOEvent> eventTable = denseTable();

    public FileIoPaneView(VBox pane) {
        configure(pane);
    }

    public FileIoPageView view() {
        return new FileIoPageView(titleLabel, recordingContextLabel, clearTimeRangeButton,
                timelineTab, durationTab, eventLogTab,
                timelineChart, histogramTable, eventTable);
    }

    private void configure(VBox pane) {
        recordingContextBar.setSpacing(8);
        recordingContextBar.getChildren().setAll(recordingContextLabel, clearTimeRangeButton);
        styles(recordingContextBar, "page-toolbar");
        timelineContainer.getChildren().setAll(timelineChart);
        tab(timelineTab, timelineContainer);
        tab(durationTab, histogramTable);
        tab(eventLogTab, eventTable);
        tabs.getTabs().setAll(timelineTab, durationTab, eventLogTab);
        configureTablePage(pane, titleLabel, recordingContextBar, tabs);
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
