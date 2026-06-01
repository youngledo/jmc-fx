package com.youngledo.jmcfx.ui.heap;

import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the JFR Heap page.
public final class HeapPaneView {

    private final Label titleLabel = new Label();
    private final TableView<HeapClassHistogram> table = denseTable();
    private final VBox timelineContainer = new VBox();
    private final TimelineChart timelineChart = new TimelineChart();

    public HeapPaneView(VBox pane) {
        configure(pane);
    }

    public HeapPageView view() {
        return new HeapPageView(titleLabel, table, timelineChart);
    }

    private void configure(VBox pane) {
        timelineContainer.getChildren().setAll(timelineChart);
        configureTablePage(pane, titleLabel, new SplitPane(table, timelineContainer));
    }

    private void configureTablePage(VBox pane, Label title, Node... content) {
        pane.setSpacing(8);
        styles(title, "view-title");
        pane.getChildren().setAll(title);
        pane.getChildren().addAll(content);
        for (Node node : content) {
            if (node instanceof TableView<?> || node instanceof TabPane || node instanceof SplitPane) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }
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
