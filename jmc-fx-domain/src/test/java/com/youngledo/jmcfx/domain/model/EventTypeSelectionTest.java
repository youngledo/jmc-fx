package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class EventTypeSelectionTest {

    @Test
    void singleSelectionUsesEventTypeIdAsStableId() {
        EventTypeSelection selection = EventTypeSelection.single("jdk.ThreadSleep", "Thread Sleep");

        assertEquals("jdk.ThreadSleep", selection.id());
        assertEquals("Thread Sleep", selection.label());
        assertEquals(List.of("jdk.ThreadSleep"), selection.eventTypeIds());
        assertEquals("jdk.ThreadSleep", selection.singleEventTypeIdOrBlank());
    }

    @Test
    void groupSelectionRequiresAtLeastOneEventType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> EventTypeSelection.group("group:flight-recorder", "Flight Recorder", List.of()));

        assertEquals("eventTypeIds must not be empty", exception.getMessage());
    }

    @Test
    void groupSelectionPreservesEventTypeOrderAndHasNoSingleId() {
        EventTypeSelection selection = EventTypeSelection.group("group:flight-recorder", "Flight Recorder",
                List.of("jdk.ActiveRecording", "jdk.ActiveSetting"));

        assertEquals("group:flight-recorder", selection.id());
        assertEquals("Flight Recorder", selection.label());
        assertEquals(List.of("jdk.ActiveRecording", "jdk.ActiveSetting"), selection.eventTypeIds());
        assertEquals("", selection.singleEventTypeIdOrBlank());
    }

    @Test
    void allSelectionUsesStableDomainOwnedId() {
        EventTypeSelection selection = EventTypeSelection.all("All Events",
                List.of("jdk.CPULoad", "jdk.ThreadSleep"));

        assertEquals(EventTypeSelection.ALL_ID, selection.id());
        assertEquals("All Events", selection.label());
        assertEquals(List.of("jdk.CPULoad", "jdk.ThreadSleep"), selection.eventTypeIds());
        assertEquals("", selection.singleEventTypeIdOrBlank());
    }
}
