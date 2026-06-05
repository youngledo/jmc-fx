package io.github.youngledo.jmcfx.ui.events;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

import io.github.youngledo.jmcfx.domain.model.EventColumn;
import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldCondition;
import io.github.youngledo.jmcfx.domain.model.EventFilter;
import io.github.youngledo.jmcfx.domain.model.EventFilterOperator;
import io.github.youngledo.jmcfx.domain.model.EventProperty;
import io.github.youngledo.jmcfx.domain.model.EventRow;
import io.github.youngledo.jmcfx.domain.model.EventSelectionProperties;
import io.github.youngledo.jmcfx.domain.model.EventStackFrame;
import io.github.youngledo.jmcfx.domain.model.EventThreadInfo;
import io.github.youngledo.jmcfx.domain.model.EventTiming;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventTypeNodeKind;
import io.github.youngledo.jmcfx.domain.model.EventTypeSelection;
import io.github.youngledo.jmcfx.ui.i18n.I18n;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import javafx.scene.control.SplitPane;

/// Controller for the recording Event Browser split table/detail page.
public final class EventsPageController {

    public static final int MIN_EVENT_TYPES_WIDTH = 180;
    public static final int DEFAULT_EVENT_TYPES_WIDTH = 260;
    public static final double MAX_EVENT_TYPES_WIDTH = 360;
    public static final double DEFAULT_EVENT_TYPES_DIVIDER_POSITION = 0.25;

    private final EventsPageView view;
    private final I18n i18n;
    private final ListChangeListener<EventTypeNode> eventTypeTreeListener = change -> rebuildEventTypeTree();
    private final ListChangeListener<EventColumn> eventColumnsListener = change -> rebuildEventColumns();
    private final ListChangeListener<io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor> fieldDescriptorsListener =
            change -> rebuildColumnsMenu();
    private final ListChangeListener<EventRow> eventRowsListener = change -> selectFirstEventRow();
    private final ChangeListener<EventDetails> selectedDetailsListener =
            (observable, oldValue, newValue) -> showEventDetails(newValue);
    private final ChangeListener<EventSelectionProperties> selectionPropertiesListener =
            (observable, oldValue, newValue) -> showSelectionProperties(newValue);
    private EventBrowserViewModel viewModel;
    private boolean eventTypesDividerInitialized;

    public EventsPageController(EventsPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        bindLocalizedText();
        configureEventBrowser();
    }

    public void bind(EventBrowserViewModel nextViewModel) {
        if (viewModel != null) {
            viewModel.eventTypeTreeProperty().removeListener(eventTypeTreeListener);
            viewModel.columnsProperty().removeListener(eventColumnsListener);
            viewModel.fieldDescriptorsProperty().removeListener(fieldDescriptorsListener);
            viewModel.rowsProperty().removeListener(eventRowsListener);
            viewModel.selectedDetailsProperty().removeListener(selectedDetailsListener);
            viewModel.selectionPropertiesProperty().removeListener(selectionPropertiesListener);
        }
        view.windowStatusLabel().textProperty().unbind();
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            view.eventTypesTree().setRoot(new TreeItem<>());
            view.eventsTable().setItems(FXCollections.emptyObservableList());
            view.eventsTable().getColumns().clear();
            view.columnsButton().getItems().clear();
            view.windowStatusLabel().setText(i18n.get("events.window.openPrompt"));
            showEventDetails(null);
            showSelectionProperties(null);
            return;
        }
        nextViewModel.eventTypeTreeProperty().addListener(eventTypeTreeListener);
        nextViewModel.columnsProperty().addListener(eventColumnsListener);
        nextViewModel.fieldDescriptorsProperty().addListener(fieldDescriptorsListener);
        nextViewModel.rowsProperty().addListener(eventRowsListener);
        nextViewModel.selectedDetailsProperty().addListener(selectedDetailsListener);
        nextViewModel.selectionPropertiesProperty().addListener(selectionPropertiesListener);
        view.eventsTable().setItems(nextViewModel.rowsProperty());
        view.windowStatusLabel().textProperty().bind(nextViewModel.statusMessageProperty());
        rebuildEventTypeTree();
        rebuildEventColumns();
        showEventDetails(nextViewModel.selectedDetailsProperty().get());
        showSelectionProperties(nextViewModel.selectionPropertiesProperty().get());
    }

    public static boolean shouldSelectEventTypesTreeNode(String eventTypeId) {
        return eventTypeId != null && !eventTypeId.isBlank() && !EventTypeSelection.ALL_ID.equals(eventTypeId);
    }

    public static boolean shouldClearEventTypesTreeSelection(String eventTypeId) {
        return !shouldSelectEventTypesTreeNode(eventTypeId);
    }

    public static boolean shouldInitializeEventTypesDivider(boolean initialized, boolean eventsVisible) {
        return !initialized && eventsVisible;
    }

    public static String formatEventTimeForDisplay(java.time.Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(zoneId)
                .format(instant);
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("events.title"));
        view.searchField().promptTextProperty().bind(i18n.text("events.search.prompt"));
        view.threadFilterField().promptTextProperty().bind(i18n.text("events.thread.prompt"));
        view.fieldFilterField().promptTextProperty().bind(i18n.text("events.field.prompt"));
        view.clearFiltersButton().textProperty().bind(i18n.text("events.filters.clear"));
        view.columnsButton().textProperty().bind(i18n.text("events.columns"));
        view.propertiesTab().textProperty().bind(i18n.text("events.details.properties"));
        view.timingTab().textProperty().bind(i18n.text("events.details.timing"));
        view.threadTab().textProperty().bind(i18n.text("events.details.thread"));
        view.stackTraceTab().textProperty().bind(i18n.text("events.details.stackTrace"));
    }

    private void configureEventBrowser() {
        view.eventTypesTree().setShowRoot(false);
        view.eventTypesTree().setMinWidth(MIN_EVENT_TYPES_WIDTH);
        view.eventTypesTree().setPrefWidth(DEFAULT_EVENT_TYPES_WIDTH);
        view.eventTypesTree().setMaxWidth(MAX_EVENT_TYPES_WIDTH);
        SplitPane.setResizableWithParent(view.eventTypesTree(), true);
        view.pane().visibleProperty().addListener((observable, oldValue, newValue) -> initializeEventTypesDivider());
        initializeEventTypesDivider();
        view.eventTypesTree().setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(EventTypeNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : eventTypeText(item));
            }
        });
        view.eventTypesTree().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> selectEventType(newValue));

        view.eventsTable().setPlaceholder(emptyTablePlaceholder());
        view.eventsTable().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> selectEventRow(newValue));

        view.clearFiltersButton().setOnAction(event -> clearEventFilters());
        view.searchField().setOnAction(event -> refreshVisibleRange());
        view.threadFilterField().setOnAction(event -> refreshVisibleRange());
        view.fieldFilterField().setOnAction(event -> refreshVisibleRange());

        configureEventPropertiesTable();
        bind(null);
    }

    private void initializeEventTypesDivider() {
        if (!shouldInitializeEventTypesDivider(eventTypesDividerInitialized, view.pane().isVisible())) {
            return;
        }
        eventTypesDividerInitialized = true;
        Platform.runLater(() -> view.splitPane().setDividerPositions(DEFAULT_EVENT_TYPES_DIVIDER_POSITION));
    }

    private void rebuildEventTypeTree() {
        if (viewModel == null) {
            view.eventTypesTree().setRoot(new TreeItem<>());
            return;
        }
        TreeItem<EventTypeNode> rootItem = new TreeItem<>();
        viewModel.eventTypeTreeProperty().stream()
                .map(this::toTreeItem)
                .forEach(rootItem.getChildren()::add);
        rootItem.setExpanded(true);
        view.eventTypesTree().setRoot(rootItem);
        selectTreeItem(rootItem, viewModel.selectedEventTypeIdProperty().get());
    }

    private TreeItem<EventTypeNode> toTreeItem(EventTypeNode node) {
        TreeItem<EventTypeNode> item = new TreeItem<>(node);
        node.children().stream()
                .map(this::toTreeItem)
                .forEach(item.getChildren()::add);
        item.setExpanded(true);
        return item;
    }

    private void selectTreeItem(TreeItem<EventTypeNode> item, String eventTypeId) {
        if (shouldClearEventTypesTreeSelection(eventTypeId)) {
            view.eventTypesTree().getSelectionModel().clearSelection();
            return;
        }
        for (TreeItem<EventTypeNode> child : item.getChildren()) {
            EventTypeNode node = child.getValue();
            if (node != null && eventTypeId.equals(node.eventTypeId())) {
                view.eventTypesTree().getSelectionModel().select(child);
                return;
            }
            selectTreeItem(child, eventTypeId);
        }
    }

    private void selectEventType(TreeItem<EventTypeNode> item) {
        if (viewModel == null) {
            return;
        }
        viewModel.selectEventTypeNode(item == null ? null : item.getValue());
    }

    private String eventTypeText(EventTypeNode node) {
        if (node.kind() != EventTypeNodeKind.EVENT_TYPE) {
            return node.label();
        }
        return node.label() + " (" + node.count() + ")";
    }

    private void rebuildEventColumns() {
        if (viewModel == null) {
            view.eventsTable().getColumns().clear();
            view.columnsButton().getItems().clear();
            return;
        }
        view.eventsTable().getColumns().setAll(viewModel.columnsProperty().stream()
                .map(this::toTableColumn)
                .toList());
        rebuildColumnsMenu();
    }

    private void rebuildColumnsMenu() {
        if (viewModel == null) {
            view.columnsButton().getItems().clear();
            return;
        }
        view.columnsButton().getItems().setAll(viewModel.fieldDescriptorsProperty().stream()
                .map(field -> {
                    CheckMenuItem item = new CheckMenuItem(field.label());
                    item.setSelected(viewModel.columnsProperty().stream()
                            .anyMatch(column -> field.id().equals(column.fieldId())));
                    item.setOnAction(event -> {
                        if (item.isSelected()) {
                            viewModel.addFieldColumn(field.id());
                        } else {
                            viewModel.removeColumn("field:" + field.id());
                        }
                    });
                    return item;
                })
                .toList());
    }

    private TableColumn<EventRow, String> toTableColumn(EventColumn column) {
        TableColumn<EventRow, String> tableColumn = new TableColumn<>(column.label());
        tableColumn.setPrefWidth(column.width());
        tableColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(columnValue(column, cell.getValue())));
        return tableColumn;
    }

    private String columnValue(EventColumn column, EventRow row) {
        return switch (column.kind()) {
            case COMMON -> commonColumnValue(column.id(), row);
            case FIELD -> row.fieldValues().getOrDefault(column.fieldId(), "");
        };
    }

    private String commonColumnValue(String columnId, EventRow row) {
        return switch (columnId) {
            case "eventType" -> row.eventTypeId();
            case "startTime" -> formatEventTime(row.startTime());
            case "duration" -> row.durationText();
            case "eventThread" -> row.threadName();
            default -> "";
        };
    }

    private String formatEventTime(java.time.Instant instant) {
        return formatEventTimeForDisplay(instant, ZoneId.systemDefault());
    }

    private void selectFirstEventRow() {
        if (view.eventsTable().getItems().isEmpty()) {
            if (viewModel != null) {
                viewModel.selectedDetailsProperty().set(null);
            }
            return;
        }
        view.eventsTable().getSelectionModel().selectFirst();
    }

    private void selectEventRow(EventRow row) {
        if (viewModel == null) {
            return;
        }
        EventDetails details = viewModel.selectedDetailsProperty().get();
        if (row == null || details == null || !row.id().equals(details.eventId())) {
            viewModel.selectRow(row);
        }
        view.detailsTabs().setDisable(row == null && viewModel.selectedDetailsProperty().get() == null);
    }

    private void clearEventFilters() {
        view.searchField().clear();
        view.threadFilterField().clear();
        view.fieldFilterField().clear();
        refreshVisibleRange();
    }

    private void refreshVisibleRange() {
        if (viewModel == null) {
            return;
        }
        viewModel.setFilter(new EventFilter(view.searchField().getText(), view.threadFilterField().getText(),
                null, null, fieldConditions(view.fieldFilterField().getText())));
    }

    private List<EventFieldCondition> fieldConditions(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        String[] parts = expression.trim().split("\\s+", 3);
        if (parts.length < 3) {
            return List.of();
        }
        EventFilterOperator operator = switch (parts[1]) {
            case "contains" -> EventFilterOperator.CONTAINS;
            case "=", "==" -> EventFilterOperator.EQUALS;
            case "!=", "<>" -> EventFilterOperator.NOT_EQUALS;
            case ">" -> EventFilterOperator.GREATER_THAN;
            case ">=" -> EventFilterOperator.GREATER_THAN_OR_EQUAL;
            case "<" -> EventFilterOperator.LESS_THAN;
            case "<=" -> EventFilterOperator.LESS_THAN_OR_EQUAL;
            default -> null;
        };
        return operator == null ? List.of() : List.of(new EventFieldCondition(parts[0], operator, parts[2]));
    }

    private void configureEventPropertiesTable() {
        view.propertiesTable().setPlaceholder(emptyTablePlaceholder());
        TableColumn<EventProperty, String> nameColumn = new TableColumn<>();
        nameColumn.textProperty().bind(i18n.text("events.properties.field"));
        nameColumn.setPrefWidth(220);
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));
        TableColumn<EventProperty, String> valueColumn = new TableColumn<>();
        valueColumn.textProperty().bind(i18n.text("events.properties.value"));
        valueColumn.setPrefWidth(420);
        valueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        view.propertiesTable().getColumns().setAll(List.of(nameColumn, valueColumn));
    }

    private void showEventDetails(EventDetails details) {
        if (details == null) {
            view.timingLabel().setText(noTimingSelectionText(i18n));
            view.threadLabel().setText(noThreadSelectionText(i18n));
            view.stackTraceList().setItems(FXCollections.emptyObservableList());
            return;
        }
        view.timingLabel().setText(timingText(details.timing()));
        view.threadLabel().setText(threadText(details.thread()));
        view.stackTraceList().setItems(FXCollections.observableArrayList(details.stackTrace().stream()
                .map(this::stackFrameText)
                .toList()));
    }

    private void showSelectionProperties(EventSelectionProperties properties) {
        if (properties == null) {
            view.propertiesTable().setItems(FXCollections.emptyObservableList());
            return;
        }
        view.propertiesTable().setItems(FXCollections.observableArrayList(properties.properties()));
    }

    private String timingText(EventTiming timing) {
        if (timing == null) {
            return i18n.get("events.details.noTiming");
        }
        return new StringJoiner("\n")
                .add(i18n.format("events.details.start", formatEventTime(timing.startTime())))
                .add(i18n.format("events.details.end", formatEventTime(timing.endTime())))
                .add(i18n.format("events.details.duration", timing.durationText()))
                .add(i18n.format("events.details.recordingOffset", timing.recordingOffsetText()))
                .toString();
    }

    private String threadText(EventThreadInfo thread) {
        if (thread == null) {
            return i18n.get("events.details.noThread");
        }
        return new StringJoiner("\n")
                .add(i18n.format("events.details.threadName", thread.name()))
                .add(i18n.format("events.details.threadId", thread.id()))
                .add(i18n.format("events.details.threadVirtual", thread.virtual()))
                .toString();
    }

    private String stackFrameText(EventStackFrame frame) {
        String location = frame.fileName() == null || frame.fileName().isBlank()
                ? ""
                : " (" + frame.fileName() + ":" + frame.lineNumber() + ")";
        return frame.typeName() + "." + frame.methodName() + location;
    }

    private static Region emptyTablePlaceholder() {
        Region placeholder = new Region();
        placeholder.setManaged(false);
        return placeholder;
    }

    private static String noTimingSelectionText(I18n i18n) {
        return i18n.get("events.details.selectTiming");
    }

    private static String noThreadSelectionText(I18n i18n) {
        return i18n.get("events.details.selectThread");
    }
}
