package io.github.youngledo.jmcfx.ui.events;

import io.github.youngledo.jmcfx.domain.model.EventProperty;
import io.github.youngledo.jmcfx.domain.model.EventRow;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the recording Event Browser page.
public final class EventsPaneView {

    private final Label eventsTitleLabel = new Label();
    private final TreeView<EventTypeNode> eventTypesTree = new TreeView<>();
    private final TextField eventSearchField = new TextField();
    private final TextField threadFilterField = new TextField();
    private final TextField fieldFilterField = new TextField();
    private final Button clearEventFiltersButton = new Button();
    private final MenuButton columnsButton = new MenuButton();
    private final SplitPane eventsSplitPane = new SplitPane();
    private final TableView<EventRow> eventsTable = denseTable();
    private final Label eventWindowStatusLabel = new Label();
    private final TabPane eventDetailsTabs = new TabPane();
    private final Tab eventPropertiesTab = tab();
    private final Tab eventTimingTab = tab();
    private final Tab eventThreadTab = tab();
    private final Tab eventStackTraceTab = tab();
    private final TableView<EventProperty> eventPropertiesTable = denseTable();
    private final Label eventTimingLabel = new Label();
    private final Label eventThreadLabel = new Label();
    private final ListView<String> eventStackTraceList = new ListView<>();

    public EventsPaneView(VBox pane) {
        configure(pane);
    }

    public EventsPageView view() {
        return new EventsPageView(pane(), eventsTitleLabel, eventTypesTree, eventSearchField,
                threadFilterField, fieldFilterField, clearEventFiltersButton, columnsButton, eventsSplitPane,
                eventsTable, eventWindowStatusLabel, eventDetailsTabs, eventPropertiesTab, eventTimingTab,
                eventThreadTab, eventStackTraceTab, eventPropertiesTable, eventTimingLabel, eventThreadLabel,
                eventStackTraceList);
    }

    private VBox pane() {
        return (VBox) eventsTitleLabel.getParent();
    }

    private void configure(VBox pane) {
        pane.setSpacing(8);
        styles(eventsTitleLabel, "view-title");
        HBox filters = hbox(8, eventSearchField, threadFilterField, fieldFilterField,
                clearEventFiltersButton, columnsButton);
        styles(filters, "event-filter-bar");
        HBox.setHgrow(eventSearchField, Priority.ALWAYS);
        styles(eventsTable, "dense-table");
        eventsSplitPane.getItems().setAll(eventTypesTree, vbox(6, eventsTable, eventWindowStatusLabel));
        VBox.setVgrow(eventsTable, Priority.ALWAYS);
        tab(eventPropertiesTab, eventPropertiesTable);
        tab(eventTimingTab, vbox(4, eventTimingLabel));
        tab(eventThreadTab, vbox(4, eventThreadLabel));
        tab(eventStackTraceTab, eventStackTraceList);
        eventDetailsTabs.getTabs().setAll(eventPropertiesTab, eventTimingTab, eventThreadTab, eventStackTraceTab);
        eventDetailsTabs.setPrefHeight(220);
        wrap(eventTimingLabel, eventThreadLabel);
        styles(eventWindowStatusLabel, "event-window-status");
        VBox.setVgrow(eventsSplitPane, Priority.ALWAYS);
        pane.getChildren().setAll(eventsTitleLabel, filters, eventsSplitPane, eventDetailsTabs);
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
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

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
