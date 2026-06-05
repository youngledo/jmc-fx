package io.github.youngledo.jmcfx.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventFilter;
import io.github.youngledo.jmcfx.domain.model.EventLoadState;
import io.github.youngledo.jmcfx.domain.model.EventSelectionProperties;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventTypeSelection;
import io.github.youngledo.jmcfx.domain.model.EventValueType;
import io.github.youngledo.jmcfx.domain.model.EventWindow;
import io.github.youngledo.jmcfx.domain.model.EventWindowRequest;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

class EventQueryServiceTest {

    @Test
    void compatibilityMethodsOpenAndCloseSessionPerCall() {
        RecordingService service = new RecordingService();
        RecordingSummary recording = recording();

        service.loadEventTypeTree(recording);
        service.loadFieldDescriptors(recording, "jdk.ThreadSleep");
        service.loadEventWindow(recording,
                new EventWindowRequest("jdk.ThreadSleep", 0, 10, 0, 0, List.of(), EventFilter.empty()));
        service.loadEventDetails(recording, "jdk.ThreadSleep#0");

        assertEquals(4, service.openCount);
        assertEquals(4, service.closeCount);
    }

    @Test
    void sessionCanReuseOneOpenForMultipleQueries() {
        RecordingService service = new RecordingService();

        try (EventQuerySession session = service.openSession(recording())) {
            session.loadEventTypeTree();
            session.loadFieldDescriptors("jdk.ThreadSleep");
            session.loadEventWindow(new EventWindowRequest("jdk.ThreadSleep", 0, 10, 0, 0, List.of(),
                    EventFilter.empty()));
            session.loadEventDetails("jdk.ThreadSleep#0");
        }

        assertEquals(1, service.openCount);
        assertEquals(1, service.closeCount);
    }

    @Test
    void stringFieldDescriptorCompatibilityWrapsSelectionAwareSessionMethod() {
        RecordingService service = new RecordingService();

        try (EventQuerySession session = service.openSession(recording())) {
            session.loadFieldDescriptors("jdk.ThreadSleep");
        }

        assertEquals(EventTypeSelection.single("jdk.ThreadSleep", "jdk.ThreadSleep"), service.lastSelection);
    }

    @Test
    void sessionContractRequiresSelectionAwareImplementations() throws Exception {
        assertTrue(EventQuerySession.class.getMethod("loadFieldDescriptors", String.class).isDefault());
        assertTrue(Modifier.isAbstract(EventQuerySession.class
                .getMethod("loadFieldDescriptors", EventTypeSelection.class).getModifiers()));
        assertTrue(Modifier.isAbstract(EventQuerySession.class
                .getMethod("loadSelectionProperties", EventTypeSelection.class).getModifiers()));
    }

    private RecordingSummary recording() {
        return new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static final class RecordingService implements EventQueryService {
        private int openCount;
        private int closeCount;
        private EventTypeSelection lastSelection;

        @Override
        public EventQuerySession openSession(RecordingSummary recording) {
            openCount++;
            return new EventQuerySession() {
                @Override
                public List<EventTypeNode> loadEventTypeTree() {
                    return List.of(EventTypeNode.group("jvm", "Java Virtual Machine",
                            List.of("Java Virtual Machine"), List.of(EventTypeNode.eventType("jdk.ThreadSleep",
                                    "Thread Sleep", List.of("Java Virtual Machine"), 1))));
                }

                @Override
                public List<EventFieldDescriptor> loadFieldDescriptors(EventTypeSelection selection) {
                    lastSelection = selection;
                    return List.of(new EventFieldDescriptor("duration", "Duration", "Event duration",
                            EventValueType.DURATION, "ns", true, true, true));
                }

                @Override
                public EventWindow loadEventWindow(EventWindowRequest request) {
                    return new EventWindow(request.eventTypeId(), request.loadStartRow(), List.of(), 0, true,
                            EventLoadState.EMPTY);
                }

                @Override
                public EventSelectionProperties loadSelectionProperties(EventTypeSelection selection) {
                    return new EventSelectionProperties(selection.id(), selection.label(), 0, List.of());
                }

                @Override
                public EventDetails loadEventDetails(String eventId) {
                    return null;
                }

                @Override
                public void close() {
                    closeCount++;
                }
            };
        }
    }
}
