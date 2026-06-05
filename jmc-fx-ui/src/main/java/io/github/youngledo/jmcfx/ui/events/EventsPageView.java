package io.github.youngledo.jmcfx.ui.events;

import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventFilterOperator;
import io.github.youngledo.jmcfx.domain.model.EventProperty;
import io.github.youngledo.jmcfx.domain.model.EventRow;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;

/// Narrow view handle for the recording Event Browser page.
public record EventsPageView(
        VBox pane,
        Label titleLabel,
        TreeView<EventTypeNode> eventTypesTree,
        TextField searchField,
        TextField threadFilterField,
        ComboBox<EventFieldDescriptor> fieldFilterField,
        ComboBox<EventFilterOperator> fieldFilterOperator,
        TextField fieldFilterValue,
        Button applyFiltersButton,
        Button clearFiltersButton,
        MenuButton columnsButton,
        SplitPane splitPane,
        TableView<EventRow> eventsTable,
        Label windowStatusLabel,
        TabPane detailsTabs,
        Tab propertiesTab,
        Tab timingTab,
        Tab threadTab,
        Tab stackTraceTab,
        TableView<EventProperty> propertiesTable,
        Label timingLabel,
        Label threadLabel,
        ListView<String> stackTraceList) {
}
