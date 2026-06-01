package com.youngledo.jmcfx.ui.locks;

import com.youngledo.jmcfx.domain.model.LockHistogram;

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

/// Code-first view for the JFR Locks page.
public final class LocksPaneView {

    private final Label titleLabel = new Label();
    private final HBox groupingBar = new HBox();
    private final Button groupByClassButton = new Button();
    private final Button groupByAddressButton = new Button();
    private final Button groupByThreadButton = new Button();
    private final TabPane tabs = new TabPane();
    private final Tab byClassTab = tab();
    private final TableView<LockHistogram> byClassTable = denseTable();
    private final Tab byAddressTab = tab();
    private final TableView<LockHistogram> byAddressTable = denseTable();
    private final Tab byThreadTab = tab();
    private final TableView<LockHistogram> byThreadTable = denseTable();

    public LocksPaneView(VBox pane) {
        configure(pane);
    }

    public LocksPageView view() {
        return new LocksPageView(titleLabel, groupByClassButton, groupByAddressButton, groupByThreadButton,
                byClassTab, byClassTable, byAddressTab, byAddressTable, byThreadTab, byThreadTable);
    }

    private void configure(VBox pane) {
        groupingBar.setSpacing(8);
        groupingBar.getChildren().setAll(groupByClassButton, groupByAddressButton, groupByThreadButton);
        styles(groupingBar, "locks-grouping-bar");
        tab(byClassTab, byClassTable);
        tab(byAddressTab, byAddressTable);
        tab(byThreadTab, byThreadTable);
        tabs.getTabs().setAll(byClassTab, byAddressTab, byThreadTab);
        configureTablePage(pane, titleLabel, groupingBar, tabs);
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
