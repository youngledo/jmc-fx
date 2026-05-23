package com.youngledo.jmcfx.ui.events;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.youngledo.jmcfx.domain.model.EventColumn;
import com.youngledo.jmcfx.domain.model.EventDetails;
import com.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import com.youngledo.jmcfx.domain.model.EventFilter;
import com.youngledo.jmcfx.domain.model.EventRow;
import com.youngledo.jmcfx.domain.model.EventSelectionProperties;
import com.youngledo.jmcfx.domain.model.EventTypeNode;
import com.youngledo.jmcfx.domain.model.EventTypeNodeKind;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.EventWindow;
import com.youngledo.jmcfx.domain.model.EventWindowRequest;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.EventQuerySession;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the windowed event browser state workflow.
public class EventBrowserViewModel implements AutoCloseable {

    private static final int DEFAULT_VISIBLE_ROWS = 100;
    private static final int PREFETCH_BEFORE = 50;
    private static final int PREFETCH_AFTER = 100;
    private static final int DEFAULT_FIELD_COLUMN_WIDTH = 140;
    private static final int EVENT_TYPE_COLUMN_WIDTH = 180;

    private final EventQueryService eventQueryService;
    private final EventBrowserBackgroundExecutor backgroundExecutor;
    private I18n i18n;
    private final AtomicLong requestSequence = new AtomicLong();
    private EventQuerySession activeSession;

    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();
    private final ObservableList<EventTypeNode> eventTypeTree = FXCollections.observableArrayList();
    private final StringProperty selectedEventTypeId = new SimpleStringProperty();
    private final ObjectProperty<EventTypeSelection> selectedEventTypeSelection = new SimpleObjectProperty<>();
    private final ObjectProperty<EventFilter> activeFilter = new SimpleObjectProperty<>(EventFilter.empty());
    private final ObjectProperty<EventSelectionProperties> selectionProperties = new SimpleObjectProperty<>();
    private final ObservableList<EventFieldDescriptor> fieldDescriptors = FXCollections.observableArrayList();
    private final ObservableList<EventColumn> columns = FXCollections.observableArrayList();
    private final ObservableList<EventRow> rows = FXCollections.observableArrayList();
    private final ObjectProperty<EventDetails> selectedDetails = new SimpleObjectProperty<>();
    private final BooleanProperty loading = new SimpleBooleanProperty();
    private final BooleanProperty error = new SimpleBooleanProperty();
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");

    public EventBrowserViewModel(EventQueryService eventQueryService) {
        this(eventQueryService, new VirtualThreadEventBrowserExecutor());
    }

    public EventBrowserViewModel(EventQueryService eventQueryService,
            EventBrowserBackgroundExecutor backgroundExecutor) {
        this(eventQueryService, backgroundExecutor, new I18n(java.util.Locale.getDefault()));
    }

    public EventBrowserViewModel(EventQueryService eventQueryService,
            EventBrowserBackgroundExecutor backgroundExecutor, I18n i18n) {
        this.eventQueryService = eventQueryService;
        this.backgroundExecutor = backgroundExecutor;
        this.i18n = i18n;
        statusMessage.set(i18n.get("events.status.openPrompt"));
    }

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public ObservableList<EventTypeNode> eventTypeTreeProperty() {
        return eventTypeTree;
    }

    public StringProperty selectedEventTypeIdProperty() {
        return selectedEventTypeId;
    }

    public ObjectProperty<EventTypeSelection> selectedEventTypeSelectionProperty() {
        return selectedEventTypeSelection;
    }

    public ObjectProperty<EventFilter> activeFilterProperty() {
        return activeFilter;
    }

    public ObjectProperty<EventSelectionProperties> selectionPropertiesProperty() {
        return selectionProperties;
    }

    public ObservableList<EventFieldDescriptor> fieldDescriptorsProperty() {
        return fieldDescriptors;
    }

    public ObservableList<EventRow> rowsProperty() {
        return rows;
    }

    public ObjectProperty<EventDetails> selectedDetailsProperty() {
        return selectedDetails;
    }

    public ObservableList<EventColumn> columnsProperty() {
        return columns;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public BooleanProperty errorProperty() {
        return error;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public void loadRecording(RecordingSummary recording) {
        long sequence = requestSequence.incrementAndGet();
        runOnFxThread(() -> {
            closeActiveSession();
            loading.set(true);
            resetEventState();
            currentRecording.set(recording);
            statusMessage.set(i18n.format("events.status.loading", recording.name()));
        });
        executeRequest(sequence, () -> {
            EventQuerySession session = eventQueryService.openSession(recording);
            List<EventTypeNode> loadedTree = session.loadEventTypeTree();

            onFxThread(() -> {
                if (stale(sequence)) {
                    session.close();
                    return;
                }
                closeActiveSession();
                activeSession = session;
                eventTypeTree.setAll(loadedTree);
                selectedEventTypeId.set("");
                selectedEventTypeSelection.set(null);
                selectionProperties.set(null);
                fieldDescriptors.clear();
                columns.clear();
                rows.clear();
                selectedDetails.set(null);
                loading.set(false);
                statusMessage.set(i18n.format("events.status.selectType", recording.name()));
            });
        });
    }

    public void showVisibleRange(int startRow, int rowCount) {
        EventQuerySession session = activeSession;
        EventTypeSelection selection = selectedEventTypeSelection.get();
        if (session == null || selection == null) {
            return;
        }
        long sequence = requestSequence.incrementAndGet();
        loading.set(true);
        clearError();
        List<String> columnFieldIds = activeFieldIds(columns);
        executeRequest(sequence, () -> {
            EventWindow window = session.loadEventWindow(
                    windowRequest(selection, startRow, rowCount, PREFETCH_BEFORE, columnFieldIds));
            EventDetails details = firstDetails(session, window);

            onFxThread(() -> {
                if (stale(sequence)) {
                    return;
                }
                rows.setAll(window.rows());
                selectedDetails.set(details);
                loading.set(false);
                statusMessage.set(i18n.format("events.status.loaded", rows.size(), selection.label()));
            });
        });
    }

    public void setFilter(EventFilter filter) {
        activeFilter.set(filter == null ? EventFilter.empty() : filter);
        showVisibleRange(0, DEFAULT_VISIBLE_ROWS);
    }

    public void selectEventType(String eventTypeId) {
        if (eventTypeId == null || eventTypeId.isBlank()) {
            return;
        }
        EventTypeSelection selection = EventTypeSelection.single(eventTypeId, eventTypeId);
        if (sameSelection(selection)) {
            return;
        }
        selectedEventTypeSelection.set(selection);
        selectedEventTypeId.set(selection.id());
        loadSelectedEventType(DEFAULT_VISIBLE_ROWS);
    }

    public void selectAllEventTypes() {
        EventTypeSelection selection = allSelection(eventTypeTree);
        if (selection == null || sameSelection(selection)) {
            return;
        }
        selectedEventTypeSelection.set(selection);
        selectedEventTypeId.set(selection.id());
        loadSelectedEventType(DEFAULT_VISIBLE_ROWS);
    }

    public void selectEventTypeNode(EventTypeNode node) {
        if (node == null) {
            selectedEventTypeSelection.set(null);
            selectedEventTypeId.set("");
            clearSelectionData();
            statusMessage.set(currentRecording.get() == null
                    ? i18n.get("events.status.openPrompt")
                    : i18n.format("events.status.selectType", currentRecording.get().name()));
            return;
        }
        EventTypeSelection selection = selectionFrom(node);
        if (selection == null) {
            selectedEventTypeSelection.set(null);
            selectedEventTypeId.set(node.id());
            clearSelectionData();
            statusMessage.set(i18n.format("events.status.noEvents", node.label()));
            return;
        }
        if (sameSelection(selection)) {
            return;
        }
        selectedEventTypeSelection.set(selection);
        selectedEventTypeId.set(selection.id());
        loadSelectedEventType(DEFAULT_VISIBLE_ROWS);
    }

    public void selectRow(EventRow row) {
        EventQuerySession session = activeSession;
        if (session == null || row == null) {
            selectedDetails.set(null);
            return;
        }
        long sequence = requestSequence.incrementAndGet();
        loading.set(true);
        clearError();
        executeRequest(sequence, () -> {
            EventDetails details = session.loadEventDetails(row.id());
            onFxThread(() -> {
                if (stale(sequence)) {
                    return;
                }
                selectedDetails.set(details);
                loading.set(false);
                statusMessage.set(i18n.get("events.status.detailsLoaded"));
            });
        });
    }

    public void addFieldColumn(String fieldId) {
        if (fieldId == null || fieldId.isBlank() || hasColumn("field:" + fieldId)) {
            return;
        }
        fieldDescriptors.stream()
                .filter(descriptor -> descriptor.id().equals(fieldId))
                .findFirst()
                .map(descriptor -> EventColumn.field(descriptor, defaultWidthFor(descriptor)))
                .ifPresent(columns::add);
    }

    public void removeColumn(String columnId) {
        columns.removeIf(column -> column.removable() && column.id().equals(columnId));
    }

    @Override
    public void close() {
        closeActiveSession();
        backgroundExecutor.close();
    }

    private void loadSelectedEventType(int rowCount) {
        EventQuerySession session = activeSession;
        EventTypeSelection selection = selectedEventTypeSelection.get();
        if (session == null || selection == null) {
            return;
        }
        long sequence = requestSequence.incrementAndGet();
        loading.set(true);
        clearError();
        executeRequest(sequence, () -> {
            List<EventFieldDescriptor> loadedFields = session.loadFieldDescriptors(selection);
            List<EventColumn> loadedColumns = initialColumns(selection, loadedFields);
            EventWindow window = session.loadEventWindow(
                    windowRequest(selection, 0, rowCount, 0, activeFieldIds(loadedColumns)));
            EventSelectionProperties loadedSelectionProperties = session.loadSelectionProperties(selection);
            EventDetails details = firstDetails(session, window);

            onFxThread(() -> {
                if (stale(sequence)) {
                    return;
                }
                fieldDescriptors.setAll(loadedFields);
                columns.setAll(loadedColumns);
                rows.setAll(window.rows());
                selectionProperties.set(loadedSelectionProperties);
                selectedDetails.set(details);
                loading.set(false);
                statusMessage.set(i18n.format("events.status.loaded", rows.size(), selection.label()));
            });
        });
    }

    private void executeRequest(long sequence, Runnable runnable) {
        try {
            backgroundExecutor.execute(() -> {
                try {
                    runnable.run();
                } catch (RuntimeException exception) {
                    handleFailure(sequence, exception);
                }
            });
        } catch (RuntimeException exception) {
            handleFailure(sequence, exception);
        }
    }

    private EventWindowRequest windowRequest(EventTypeSelection selection, int startRow, int rowCount, int prefetchBefore,
            List<String> columnFieldIds) {
        return new EventWindowRequest(selection, startRow, rowCount, prefetchBefore, PREFETCH_AFTER,
                columnFieldIds, activeFilter.get());
    }

    private List<String> activeFieldIds(List<EventColumn> activeColumns) {
        return activeColumns.stream()
                .filter(column -> column.kind() == com.youngledo.jmcfx.domain.model.EventColumnKind.FIELD)
                .map(EventColumn::fieldId)
                .toList();
    }

    private EventDetails firstDetails(EventQuerySession session, EventWindow window) {
        if (window == null || window.rows().isEmpty()) {
            return null;
        }
        return session.loadEventDetails(window.rows().getFirst().id());
    }

    private List<EventColumn> initialColumns(EventTypeSelection selection, List<EventFieldDescriptor> descriptors) {
        List<EventColumn> fieldColumns = descriptors.stream()
                .filter(EventFieldDescriptor::recommendedColumn)
                .map(descriptor -> EventColumn.field(descriptor, defaultWidthFor(descriptor)))
                .toList();
        if (selection != null && !selection.singleType()) {
            java.util.ArrayList<EventColumn> columns = new java.util.ArrayList<>();
            columns.add(EventColumn.common("eventType", i18n.get("events.column.eventType"), EVENT_TYPE_COLUMN_WIDTH));
            columns.addAll(fieldColumns);
            return List.copyOf(columns);
        }
        return fieldColumns;
    }

    private int defaultWidthFor(EventFieldDescriptor descriptor) {
        return switch (descriptor.valueType()) {
            case TIMESTAMP -> 180;
            case DURATION -> 110;
            default -> DEFAULT_FIELD_COLUMN_WIDTH;
        };
    }

    private boolean hasColumn(String columnId) {
        return columns.stream().anyMatch(column -> column.id().equals(columnId));
    }

    private EventTypeSelection allSelection(List<EventTypeNode> nodes) {
        List<String> eventTypeIds = nodes.stream()
                .flatMap(node -> descendantEventTypeIds(node).stream())
                .distinct()
                .toList();
        return eventTypeIds.isEmpty() ? null : EventTypeSelection.all("All Events", eventTypeIds);
    }

    private EventTypeSelection selectionFrom(EventTypeNode node) {
        if (node.kind() == EventTypeNodeKind.EVENT_TYPE) {
            return EventTypeSelection.single(node.eventTypeId(), node.label());
        }
        List<String> eventTypeIds = descendantEventTypeIds(node);
        return eventTypeIds.isEmpty() ? null : EventTypeSelection.group(node.id(), node.label(), eventTypeIds);
    }

    private boolean sameSelection(EventTypeSelection selection) {
        EventTypeSelection selected = selectedEventTypeSelection.get();
        return selected != null && selected.id().equals(selection.id());
    }

    private List<String> descendantEventTypeIds(EventTypeNode node) {
        ArrayDeque<EventTypeNode> queue = new ArrayDeque<>(List.of(node));
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            EventTypeNode current = queue.removeFirst();
            if (current.kind() == EventTypeNodeKind.EVENT_TYPE) {
                ids.add(current.eventTypeId());
            } else {
                queue.addAll(current.children());
            }
        }
        return List.copyOf(ids);
    }

    private boolean stale(long sequence) {
        return sequence != requestSequence.get();
    }

    private void clearError() {
        error.set(false);
        errorMessage.set("");
    }

    private void clearSelectionData() {
        fieldDescriptors.clear();
        columns.clear();
        rows.clear();
        selectedDetails.set(null);
        selectionProperties.set(null);
    }

    private void resetEventState() {
        clearError();
        eventTypeTree.clear();
        selectedEventTypeId.set("");
        selectedEventTypeSelection.set(null);
        activeFilter.set(EventFilter.empty());
        selectionProperties.set(null);
        fieldDescriptors.clear();
        columns.clear();
        rows.clear();
        selectedDetails.set(null);
    }

    private void closeActiveSession() {
        EventQuerySession session = activeSession;
        activeSession = null;
        if (session != null) {
            session.close();
        }
    }

    private void handleFailure(long sequence, RuntimeException exception) {
        onFxThread(() -> {
            if (stale(sequence)) {
                return;
            }
            loading.set(false);
            error.set(true);
            clearSelectionData();
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            errorMessage.set(message);
            statusMessage.set(i18n.format("events.status.loadFailed", message));
        });
    }

    private void onFxThread(Runnable runnable) {
        if (backgroundExecutor instanceof DirectEventBrowserExecutor) {
            runnable.run();
            return;
        }
        try {
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            } else {
                Platform.runLater(runnable);
            }
        } catch (RuntimeException exception) {
            runnable.run();
        }
    }

    private void runOnFxThread(Runnable runnable) {
        if (backgroundExecutor instanceof DirectEventBrowserExecutor) {
            runnable.run();
            return;
        }
        FxDispatch.run(runnable);
    }

}
