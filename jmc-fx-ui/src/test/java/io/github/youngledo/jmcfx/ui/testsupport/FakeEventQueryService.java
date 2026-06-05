package io.github.youngledo.jmcfx.ui.testsupport;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventLoadState;
import io.github.youngledo.jmcfx.domain.model.EventProperty;
import io.github.youngledo.jmcfx.domain.model.EventRow;
import io.github.youngledo.jmcfx.domain.model.EventSelectionProperties;
import io.github.youngledo.jmcfx.domain.model.EventStackFrame;
import io.github.youngledo.jmcfx.domain.model.EventThreadInfo;
import io.github.youngledo.jmcfx.domain.model.EventTiming;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventTypeSelection;
import io.github.youngledo.jmcfx.domain.model.EventValueType;
import io.github.youngledo.jmcfx.domain.model.EventWindow;
import io.github.youngledo.jmcfx.domain.model.EventWindowRequest;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.EventQueryService;
import io.github.youngledo.jmcfx.domain.service.EventQuerySession;

public class FakeEventQueryService implements EventQueryService {

    private EventWindowRequest lastWindowRequest;
    private int openSessionCount;
    private int closeSessionCount;

    @Override
    public EventQuerySession openSession(RecordingSummary recording) {
        openSessionCount++;
        return new EventQuerySession() {
            @Override
            public List<EventTypeNode> loadEventTypeTree() {
                return FakeEventQueryService.this.loadEventTypeTree(recording);
            }

            @Override
            public List<EventFieldDescriptor> loadFieldDescriptors(EventTypeSelection selection) {
                return FakeEventQueryService.this.loadFieldDescriptors(recording, selection.singleEventTypeIdOrBlank());
            }

            @Override
            public EventWindow loadEventWindow(EventWindowRequest request) {
                return FakeEventQueryService.this.loadEventWindow(recording, request);
            }

            @Override
            public EventSelectionProperties loadSelectionProperties(EventTypeSelection selection) {
                return FakeEventQueryService.this.loadSelectionProperties(selection);
            }

            @Override
            public EventDetails loadEventDetails(String eventId) {
                return FakeEventQueryService.this.loadEventDetails(recording, eventId);
            }

            @Override
            public void close() {
                closeSessionCount++;
            }
        };
    }

    @Override
    public List<EventTypeNode> loadEventTypeTree(RecordingSummary recording) {
        return List.of(EventTypeNode.group("operating-system", "Operating System", List.of("Operating System"),
                List.of(
                        EventTypeNode.eventType("jdk.CPULoad", "CPU Load", List.of("Operating System"), 1),
                        EventTypeNode.eventType("jdk.ThreadStart", "Thread Start", List.of("Operating System"), 1))));
    }

    @Override
    public List<EventFieldDescriptor> loadFieldDescriptors(RecordingSummary recording, String eventTypeId) {
        return List.of(
                new EventFieldDescriptor("startTime", "Start Time", "Event start time", EventValueType.TIMESTAMP,
                        "", true, true, true),
                new EventFieldDescriptor("duration", "Duration", "Event duration", EventValueType.DURATION,
                        "", true, true, true),
                new EventFieldDescriptor("jvmUser", "JVM User", "JVM user CPU load", EventValueType.NUMBER,
                        "", true, true, true));
    }

    @Override
    public EventWindow loadEventWindow(RecordingSummary recording, EventWindowRequest request) {
        lastWindowRequest = request;
        String eventTypeId = request.selection().eventTypeIds().getFirst();
        EventRow row = new EventRow(eventTypeId + "#0", eventTypeId, Instant.EPOCH,
                "1970-01-01T00:00:00Z", 0, "0 ns", "JVM Periodic Tasks", Map.of("jvmUser", "0.12"));
        return new EventWindow(request.eventTypeId(), request.loadStartRow(), List.of(row), 1, true,
                EventLoadState.COMPLETE);
    }

    public EventSelectionProperties loadSelectionProperties(EventTypeSelection selection) {
        return new EventSelectionProperties(selection.id(), selection.label(), selection.eventTypeIds().size(),
                List.of(new EventProperty("eventCount", "Event Count",
                        String.valueOf(selection.eventTypeIds().size()), "", "Selected event type count")));
    }

    @Override
    public EventDetails loadEventDetails(RecordingSummary recording, String eventId) {
        return new EventDetails(eventId, "jdk.CPULoad",
                List.of(new EventProperty("jvmUser", "JVM User", "0.12", "", "JVM user CPU load")),
                new EventTiming(Instant.EPOCH, Instant.EPOCH, 0, "0 ns", "0 ns"),
                new EventThreadInfo("JVM Periodic Tasks", "7", false),
                List.of(new EventStackFrame("com.example.App", "run", "App.java", 42)));
    }

    public EventWindowRequest lastWindowRequest() {
        return lastWindowRequest;
    }

    public int openSessionCount() {
        return openSessionCount;
    }

    public int closeSessionCount() {
        return closeSessionCount;
    }
}
