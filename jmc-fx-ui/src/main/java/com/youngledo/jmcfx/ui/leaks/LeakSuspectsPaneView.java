package com.youngledo.jmcfx.ui.leaks;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the JFR Leak Suspects page.
public final class LeakSuspectsPaneView {

    private final Label titleLabel = new Label();
    private final TableView<LeakCandidate> table = denseTable();
    private final TreeView<LeakReferenceNode> referenceTree = new TreeView<>();

    public LeakSuspectsPaneView(VBox pane) {
        configure(pane);
    }

    public LeakSuspectsPageView view() {
        return new LeakSuspectsPageView(titleLabel, table, referenceTree);
    }

    private void configure(VBox pane) {
        configureTablePage(pane, titleLabel, new SplitPane(table, referenceTree));
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
