package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class EventWindowRequestTest {

    @Test
    void derivesLoadedRangeFromVisibleRangeAndPrefetch() {
        EventWindowRequest request = new EventWindowRequest("jdk.CPULoad", 100, 50, 25, 75,
                List.of("jvmUser"), EventFilter.empty());

        assertEquals(75, request.loadStartRow());
        assertEquals(150, request.loadRowCount());
    }

    @Test
    void clampsLoadedRangeAtZero() {
        EventWindowRequest request = new EventWindowRequest("jdk.CPULoad", 10, 40, 25, 25,
                List.of(), EventFilter.empty());

        assertEquals(0, request.loadStartRow());
        assertEquals(75, request.loadRowCount());
    }

    @Test
    void requestAcceptsMultiTypeSelection() {
        EventTypeSelection selection = EventTypeSelection.group("group:runtime", "Runtime",
                List.of("jdk.CPULoad", "jdk.ThreadSleep"));

        EventWindowRequest request = new EventWindowRequest(selection, 20, 50, 10, 15, List.of("startTime"),
                EventFilter.empty());

        assertEquals(selection, request.selection());
        assertEquals("group:runtime", request.eventTypeId());
        assertEquals(10, request.loadStartRow());
        assertEquals(75, request.loadRowCount());
    }

    @Test
    void rejectsBlankEventTypeId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventWindowRequest("", 0, 100, 0, 0, List.of(), EventFilter.empty()));
    }
}
