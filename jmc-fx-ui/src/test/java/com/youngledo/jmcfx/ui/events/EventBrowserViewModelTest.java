package com.youngledo.jmcfx.ui.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.EventColumnKind;
import com.youngledo.jmcfx.domain.model.EventDetails;
import com.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import com.youngledo.jmcfx.domain.model.EventFilter;
import com.youngledo.jmcfx.domain.model.EventLoadState;
import com.youngledo.jmcfx.domain.model.EventProperty;
import com.youngledo.jmcfx.domain.model.EventRow;
import com.youngledo.jmcfx.domain.model.EventSelectionProperties;
import com.youngledo.jmcfx.domain.model.EventStackFrame;
import com.youngledo.jmcfx.domain.model.EventThreadInfo;
import com.youngledo.jmcfx.domain.model.EventTiming;
import com.youngledo.jmcfx.domain.model.EventTypeNode;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.EventValueType;
import com.youngledo.jmcfx.domain.model.EventWindow;
import com.youngledo.jmcfx.domain.model.EventWindowRequest;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.EventQuerySession;
import com.youngledo.jmcfx.testsupport.FakeEventQueryService;

import javafx.application.Platform;

class EventBrowserViewModelTest {

    @Test
    void loadingRecordingLoadsTreeWithoutSelectingTypeOrRows() {
        FakeEventQueryService service = new FakeEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());

        assertEquals("Operating System", viewModel.eventTypeTreeProperty().getFirst().label());
        assertEquals("", viewModel.selectedEventTypeIdProperty().get());
        assertNull(viewModel.selectedEventTypeSelectionProperty().get());
        assertTrue(viewModel.fieldDescriptorsProperty().isEmpty());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains("Select an event type"));
    }

    @Test
    void selectingAllEventsUsesNoPrefetchBeforeAndRecommendedFieldColumns() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();

        assertEquals(0, service.lastWindowRequest().visibleStartRow());
        assertEquals(100, service.lastWindowRequest().visibleRowCount());
        assertEquals(0, service.lastWindowRequest().prefetchBefore());
        assertEquals(100, service.lastWindowRequest().prefetchAfter());
        assertEquals(List.of("startTime", "duration", "jvmUser"), service.lastWindowRequest().columnFieldIds());
    }

    @Test
    void defaultColumnsComeDirectlyFromSelectedTypeDescriptors() {
        EventBrowserViewModel viewModel = new EventBrowserViewModel(new FakeEventQueryService(),
                new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();

        assertEquals(List.of("Event Type", "Start Time", "Duration", "JVM User"),
                viewModel.columnsProperty().stream().map(com.youngledo.jmcfx.domain.model.EventColumn::label).toList());
        assertEquals(EventColumnKind.COMMON, viewModel.columnsProperty().getFirst().kind());
        assertTrue(viewModel.columnsProperty().stream().skip(1)
                .allMatch(column -> column.kind() == EventColumnKind.FIELD));
    }

    @Test
    void explicitAllSelectionRepresentsAllEventTypesInRecording() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();

        assertEquals(EventTypeSelection.ALL_ID, service.lastWindowRequest().selection().id());
        assertEquals(List.of("rec.CPULoad", "rec.ThreadStart"),
                service.lastWindowRequest().selection().eventTypeIds());
        assertEquals("Event Type", viewModel.columnsProperty().getFirst().label());
    }

    @Test
    void eventThreadIsAFieldColumnWhenJmcDescriptorExposesIt() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectEventType("rec.ThreadStart");

        assertEquals(List.of("Start Time", "Duration", "Event Thread", "Thread Id"),
                viewModel.columnsProperty().stream().map(com.youngledo.jmcfx.domain.model.EventColumn::label).toList());
        assertTrue(viewModel.columnsProperty().stream()
                .allMatch(column -> column.kind() == EventColumnKind.FIELD));
    }

    @Test
    void visibleRangeRequestsWindowWithoutPaginationState() {
        FakeEventQueryService service = new FakeEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();
        viewModel.showVisibleRange(100, 40);

        assertEquals(100, service.lastWindowRequest().visibleStartRow());
        assertEquals(40, service.lastWindowRequest().visibleRowCount());
        assertEquals(50, service.lastWindowRequest().prefetchBefore());
        assertEquals(100, service.lastWindowRequest().prefetchAfter());
    }

    @Test
    void activeFilterIsIncludedInWindowRequests() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();
        viewModel.setFilter(new EventFilter("Thread Start", "main", null, null, List.of()));

        assertEquals("Thread Start", service.lastWindowRequest().filter().text());
        assertEquals("main", service.lastWindowRequest().filter().thread());
    }

    @Test
    void activeFieldColumnsDriveLaterWindowRequests() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();
        viewModel.addFieldColumn("machineTotal");
        viewModel.showVisibleRange(10, 20);
        assertEquals(List.of("startTime", "duration", "jvmUser", "machineTotal"),
                service.lastWindowRequest().columnFieldIds());

        viewModel.removeColumn("field:jvmUser");
        viewModel.showVisibleRange(20, 20);

        assertEquals(List.of("startTime", "duration", "machineTotal"), service.lastWindowRequest().columnFieldIds());
    }

    @Test
    void selectingEventTypeReloadsFieldsColumnsInitialWindowAndDetails() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectEventType("rec.ThreadStart");

        assertEquals("rec.ThreadStart", viewModel.selectedEventTypeIdProperty().get());
        assertEquals("startTime", viewModel.fieldDescriptorsProperty().getFirst().id());
        assertEquals("rec.ThreadStart", service.lastWindowRequest().eventTypeId());
        assertEquals(0, service.lastWindowRequest().visibleStartRow());
        assertEquals(100, service.lastWindowRequest().visibleRowCount());
        assertEquals(List.of("startTime", "duration", "eventThread", "threadId"),
                service.lastWindowRequest().columnFieldIds());
        assertEquals("rec.ThreadStart", viewModel.rowsProperty().getFirst().eventTypeId());
        assertEquals("rec.ThreadStart#0", viewModel.selectedDetailsProperty().get().eventId());
    }

    @Test
    void selectingGroupLoadsWindowForDescendantEventTypes() {
        FakeEventQueryService service = new FakeEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());

        EventTypeNode group = viewModel.eventTypeTreeProperty().stream()
                .filter(node -> node.kind() == com.youngledo.jmcfx.domain.model.EventTypeNodeKind.GROUP)
                .findFirst()
                .orElseThrow();
        viewModel.selectEventTypeNode(group);

        assertEquals(group.id(), service.lastWindowRequest().selection().id());
        assertTrue(service.lastWindowRequest().selection().eventTypeIds().size() > 1);
        assertTrue(viewModel.columnsProperty().stream()
                .anyMatch(column -> column.kind() == EventColumnKind.COMMON
                        && column.id().equals("eventType")
                        && column.label().equals("Event Type")));
    }

    @Test
    void selectionPropertiesUpdateWhenSelectingEventTypesNode() {
        FakeEventQueryService service = new FakeEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());

        EventTypeNode group = viewModel.eventTypeTreeProperty().stream()
                .filter(node -> node.kind() == com.youngledo.jmcfx.domain.model.EventTypeNodeKind.GROUP)
                .findFirst()
                .orElseThrow();
        viewModel.selectEventTypeNode(group);

        assertFalse(viewModel.selectionPropertiesProperty().get().properties().isEmpty());
        assertEquals(group.id(), viewModel.selectionPropertiesProperty().get().selectionId());
    }

    @Test
    void selectingEmptyGroupDoesNotCrashOrReload() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());
        EventWindowRequest initialRequest = service.lastWindowRequest();
        EventTypeNode emptyGroup = EventTypeNode.group("empty", "Empty", List.of("Empty"), List.of());

        assertDoesNotThrow(() -> viewModel.selectEventTypeNode(emptyGroup));

        assertEquals(initialRequest, service.lastWindowRequest());
        assertEquals("empty", viewModel.selectedEventTypeIdProperty().get());
        assertNull(viewModel.selectedEventTypeSelectionProperty().get());
    }

    @Test
    void failedSelectionClearsStaleRowsDetailsAndProperties() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());
        viewModel.selectEventType("rec.ThreadStart");
        assertFalse(viewModel.rowsProperty().isEmpty());
        assertNotNull(viewModel.selectionPropertiesProperty().get());

        service.failWindows = true;
        viewModel.selectEventType("rec.CPULoad");

        assertTrue(viewModel.errorProperty().get());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());
        assertNull(viewModel.selectionPropertiesProperty().get());
    }

    @Test
    void selectingSameNodeDoesNotReloadWindow() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());

        EventTypeNode group = viewModel.eventTypeTreeProperty().getFirst();
        viewModel.selectEventTypeNode(group);
        int windowLoadCount = service.windowLoadCount();

        viewModel.selectEventTypeNode(group);

        assertEquals(windowLoadCount, service.windowLoadCount());
    }

    @Test
    void clearingEventTypeSelectionDoesNotSelectAllEvents() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());

        viewModel.selectEventTypeNode(null);

        assertEquals("", viewModel.selectedEventTypeIdProperty().get());
        assertNull(viewModel.selectedEventTypeSelectionProperty().get());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(service.lastWindowRequest());
    }

    @Test
    void selectingRowLoadsDetailsForThatEvent() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        EventRow row = new EventRow("rec.CPULoad#42", "rec.CPULoad", Instant.EPOCH,
                "1970-01-01T00:00:00Z", 0, "0 ns", "JVM Periodic Tasks", Map.of("jvmUser", "0.99"));

        viewModel.selectRow(row);

        assertEquals("rec.CPULoad#42", viewModel.selectedDetailsProperty().get().eventId());
        assertEquals("rec.CPULoad#42", service.lastDetailsEventId());
    }

    @Test
    void addsAndRemovesFieldColumnsForCurrentSession() {
        EventBrowserViewModel viewModel = new EventBrowserViewModel(new FakeEventQueryService(),
                new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();
        viewModel.addFieldColumn("jvmUser");
        viewModel.removeColumn("field:jvmUser");

        assertEquals(EventColumnKind.COMMON, viewModel.columnsProperty().getFirst().kind());
        assertTrue(viewModel.columnsProperty().stream().skip(1)
                .allMatch(column -> column.kind() == EventColumnKind.FIELD));
        assertFalse(viewModel.columnsProperty().stream().anyMatch(column -> column.id().equals("field:jvmUser")));
    }

    @Test
    void serviceFailureClearsLoadingSetsErrorAndStaleSelectionData() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());
        viewModel.loadRecording(recording());
        viewModel.selectAllEventTypes();
        service.failWindows = true;

        viewModel.showVisibleRange(10, 20);

        assertFalse(viewModel.loadingProperty().get());
        assertTrue(viewModel.errorProperty().get());
        assertTrue(viewModel.errorMessageProperty().get().contains("Window failed"));
        assertTrue(viewModel.statusMessageProperty().get().contains("Window failed"));
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());
        assertNull(viewModel.selectionPropertiesProperty().get());
    }

    @Test
    void loadingNewRecordingClearsOldEventStateBeforeBackgroundWork() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        ControllableEventBrowserExecutor executor = new ControllableEventBrowserExecutor();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, executor);

        viewModel.loadRecording(recording());
        runAndDrainFxEvents(executor::runLast);
        viewModel.selectAllEventTypes();
        runAndDrainFxEvents(executor::runLast);
        assertFalse(viewModel.rowsProperty().isEmpty());
        assertNotNull(viewModel.selectedDetailsProperty().get());
        service.failWindows = true;

        viewModel.loadRecording(new RecordingSummary("new", Path.of("new.jfr"), "new.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(2), 2000, 256));
        viewModel.showVisibleRange(10, 20);

        assertEquals("", viewModel.selectedEventTypeIdProperty().get());
        assertTrue(viewModel.fieldDescriptorsProperty().isEmpty());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertTrue(viewModel.eventTypeTreeProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());
        assertEquals(1, executor.pendingCount());

        runAndDrainFxEvents(executor::runLast);

        assertFalse(viewModel.loadingProperty().get());
        assertFalse(viewModel.errorProperty().get());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());
        assertEquals("Operating System new", viewModel.eventTypeTreeProperty().getFirst().label());
    }

    @Test
    void loadingRecordingFromBackgroundThreadUpdatesInitialStateOnFxThread() throws Exception {
        ensureFxToolkit();
        RecordingEventQueryService service = new RecordingEventQueryService();
        ControllableEventBrowserExecutor executor = new ControllableEventBrowserExecutor();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, executor);
        CountDownLatch statusChanged = new CountDownLatch(1);
        AtomicReference<Thread> fxThread = new AtomicReference<>();
        AtomicReference<Thread> listenerThread = new AtomicReference<>();
        CountDownLatch fxThreadCaptured = new CountDownLatch(1);
        Platform.runLater(() -> {
            fxThread.set(Thread.currentThread());
            fxThreadCaptured.countDown();
        });
        assertTrue(fxThreadCaptured.await(5, TimeUnit.SECONDS));
        viewModel.statusMessageProperty().addListener((obs, old, val) -> {
            if (val.contains("Loading")) {
                listenerThread.set(Thread.currentThread());
                statusChanged.countDown();
            }
        });

        Thread backgroundThread = new Thread(() -> viewModel.loadRecording(recording()));
        backgroundThread.start();
        backgroundThread.join();

        assertTrue(statusChanged.await(5, TimeUnit.SECONDS));
        assertEquals(fxThread.get(), listenerThread.get());
    }

    @Test
    void staleRequestsDoNotReplaceNewerResults() {
        RecordingEventQueryService service = new RecordingEventQueryService();
        ControllableEventBrowserExecutor executor = new ControllableEventBrowserExecutor();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, executor);

        viewModel.loadRecording(recording());
        viewModel.loadRecording(new RecordingSummary("new", Path.of("new.jfr"), "new.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(2), 2000, 256));

        runAndDrainFxEvents(executor::runLast);
        assertEquals("new", viewModel.currentRecordingProperty().get().id());
        assertEquals("", viewModel.selectedEventTypeIdProperty().get());
        assertEquals("Operating System new", viewModel.eventTypeTreeProperty().getFirst().label());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());

        runAndDrainFxEvents(executor::runFirst);
        assertEquals("new", viewModel.currentRecordingProperty().get().id());
        assertEquals("", viewModel.selectedEventTypeIdProperty().get());
        assertEquals("Operating System new", viewModel.eventTypeTreeProperty().getFirst().label());
        assertTrue(viewModel.rowsProperty().isEmpty());
        assertNull(viewModel.selectedDetailsProperty().get());
    }

    @Test
    void loadingReplacementRecordingClosesPreviousSession() {
        FakeEventQueryService service = new FakeEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.loadRecording(new RecordingSummary("new", Path.of("new.jfr"), "new.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(2), 2000, 256));

        assertEquals(2, service.openSessionCount());
        assertEquals(1, service.closeSessionCount());
    }

    @Test
    void closingViewModelClosesActiveSession() {
        FakeEventQueryService service = new FakeEventQueryService();
        EventBrowserViewModel viewModel = new EventBrowserViewModel(service, new DirectEventBrowserExecutor());

        viewModel.loadRecording(recording());
        viewModel.close();

        assertEquals(1, service.openSessionCount());
        assertEquals(1, service.closeSessionCount());
    }

    private RecordingSummary recording() {
        return new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static void runAndDrainFxEvents(Runnable runnable) {
        runnable.run();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.runLater(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for JavaFX events");
            }
        } catch (IllegalStateException exception) {
            latch.countDown();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for JavaFX events", exception);
        }
    }

    private static void ensureFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException exception) {
            started.countDown();
        }
        if (!started.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out starting JavaFX toolkit");
        }
    }

    private static final class ControllableEventBrowserExecutor implements EventBrowserBackgroundExecutor {
        private final ArrayDeque<Runnable> runnables = new ArrayDeque<>();

        @Override
        public void execute(Runnable runnable) {
            runnables.addLast(runnable);
        }

        void runFirst() {
            runnables.removeFirst().run();
        }

        void runLast() {
            runnables.removeLast().run();
        }

        int pendingCount() {
            return runnables.size();
        }
    }

    private static class RecordingEventQueryService implements EventQueryService {
        private EventWindowRequest lastWindowRequest;
        private String lastDetailsEventId;
        private boolean failWindows;
        private int windowLoadCount;

        @Override
        public EventQuerySession openSession(RecordingSummary recording) {
            return new EventQuerySession() {
                @Override
                public List<EventTypeNode> loadEventTypeTree() {
                    return RecordingEventQueryService.this.tree(recording);
                }

                @Override
                public List<EventFieldDescriptor> loadFieldDescriptors(EventTypeSelection selection) {
                    return RecordingEventQueryService.this.fields(recording, selection.singleEventTypeIdOrBlank());
                }

                @Override
                public EventWindow loadEventWindow(EventWindowRequest request) {
                    return RecordingEventQueryService.this.window(recording, request);
                }

                @Override
                public EventSelectionProperties loadSelectionProperties(EventTypeSelection selection) {
                    return new EventSelectionProperties(selection.id(), selection.label(), 0, List.of());
                }

                @Override
                public EventDetails loadEventDetails(String eventId) {
                    return RecordingEventQueryService.this.details(recording, eventId);
                }

                @Override
                public void close() {
                }
            };
        }

        private List<EventTypeNode> tree(RecordingSummary recording) {
            String category = "Operating System " + recording.id();
            return List.of(EventTypeNode.group("operating-system-" + recording.id(), category, List.of(category),
                    List.of(
                            EventTypeNode.eventType(recording.id() + ".CPULoad", "CPU Load " + recording.id(),
                                    List.of(category), 1),
                            EventTypeNode.eventType(recording.id() + ".ThreadStart",
                            "Thread Start " + recording.id(), List.of(category), 1))));
        }

        private List<EventFieldDescriptor> fields(RecordingSummary recording, String eventTypeId) {
            if (eventTypeId.endsWith(".ThreadStart")) {
                return List.of(
                        new EventFieldDescriptor("startTime", "Start Time", "Event start time",
                                EventValueType.TIMESTAMP, "", true, true, true),
                        new EventFieldDescriptor("duration", "Duration", "Event duration",
                                EventValueType.DURATION, "", true, true, true),
                        new EventFieldDescriptor("eventThread", "Event Thread", "Event thread",
                                EventValueType.TEXT, "", true, true, true),
                        new EventFieldDescriptor("threadId", "Thread Id", "Started thread id",
                                EventValueType.TEXT, "", true, true, true));
            }
            return List.of(
                    new EventFieldDescriptor("startTime", "Start Time", "Event start time",
                            EventValueType.TIMESTAMP, "", true, true, true),
                    new EventFieldDescriptor("duration", "Duration", "Event duration",
                            EventValueType.DURATION, "", true, true, true),
                    new EventFieldDescriptor("jvmUser", "JVM User", "JVM user CPU load", EventValueType.NUMBER,
                            "", true, true, true),
                    new EventFieldDescriptor("machineTotal", "Machine Total", "Machine CPU load",
                            EventValueType.NUMBER, "", false, true, true));
        }

        private EventWindow window(RecordingSummary recording, EventWindowRequest request) {
            if (failWindows) {
                throw new IllegalStateException("Window failed");
            }
            lastWindowRequest = request;
            windowLoadCount++;
            String rowEventTypeId = request.selection().eventTypeIds().getFirst();
            Map<String, String> values = rowEventTypeId.endsWith(".ThreadStart")
                    ? Map.of("threadId", "7")
                    : Map.of("jvmUser", "0.12", "machineTotal", "0.42");
            EventRow row = new EventRow(rowEventTypeId + "#0", rowEventTypeId, Instant.EPOCH,
                    "1970-01-01T00:00:00Z", 0, "0 ns", "JVM Periodic Tasks", values);
            return new EventWindow(request.eventTypeId(), request.loadStartRow(), List.of(row), 1, true,
                    EventLoadState.COMPLETE);
        }

        private EventDetails details(RecordingSummary recording, String eventId) {
            lastDetailsEventId = eventId;
            String eventTypeId = eventId.substring(0, eventId.indexOf('#'));
            return new EventDetails(eventId, eventTypeId,
                    List.of(new EventProperty("jvmUser", "JVM User", "0.12", "", "JVM user CPU load")),
                    new EventTiming(Instant.EPOCH, Instant.EPOCH, 0, "0 ns", "0 ns"),
                    new EventThreadInfo("JVM Periodic Tasks", "7", false),
                    List.of(new EventStackFrame("com.example.App", "run", "App.java", 42)));
        }

        EventWindowRequest lastWindowRequest() {
            return lastWindowRequest;
        }

        String lastDetailsEventId() {
            return lastDetailsEventId;
        }

        int windowLoadCount() {
            return windowLoadCount;
        }
    }
}
