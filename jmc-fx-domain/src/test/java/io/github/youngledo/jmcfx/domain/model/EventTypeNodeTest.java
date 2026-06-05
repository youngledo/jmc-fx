package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EventTypeNodeTest {

    @Test
    void createsJmcTopLevelGroupWithChildren() {
        EventTypeNode child = EventTypeNode.eventType("jdk.CPULoad", "CPU Load", List.of("Operating System"), 10);
        EventTypeNode root = EventTypeNode.group("operating-system", "Operating System", List.of("Operating System"),
                List.of(child));

        assertEquals(EventTypeNodeKind.GROUP, root.kind());
        assertEquals("Operating System", root.label());
        assertEquals(List.of("Operating System"), root.categoryPath());
        assertEquals(10, root.count());
        assertEquals(child, root.children().getFirst());
        assertTrue(child.children().isEmpty());
    }

    @Test
    void groupCountIgnoresUnknownChildCounts() {
        EventTypeNode known = EventTypeNode.eventType("jdk.CPULoad", "CPU Load", List.of("Operating System"), 10);
        EventTypeNode unknown = EventTypeNode.group("unknown", "Unknown", List.of("Unknown"), -1, List.of());
        EventTypeNode root = EventTypeNode.group("root", "Root", List.of("Root"), List.of(known, unknown));

        assertEquals(10, root.count());
    }

    @Test
    void rejectsGroupWithoutChildrenFactoryForEventType() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventTypeNode("x", "", EventTypeNodeKind.EVENT_TYPE, List.of(), "", -1, List.of()));
    }

    @Test
    void rejectsEventTypeWithBlankEventTypeId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventTypeNode("x", "X", EventTypeNodeKind.EVENT_TYPE, List.of(), "", 0, List.of()));
    }

    @Test
    void rejectsEventTypeWithChildren() {
        EventTypeNode child = EventTypeNode.eventType("jdk.CPULoad", "CPU Load", List.of("Operating System"), 10);

        assertThrows(IllegalArgumentException.class,
                () -> new EventTypeNode("x", "X", EventTypeNodeKind.EVENT_TYPE, List.of(), "x", 0, List.of(child)));
    }

    @Test
    void rejectsEventTypeWithNegativeCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventTypeNode("x", "X", EventTypeNodeKind.EVENT_TYPE, List.of(), "x", -1, List.of()));
    }

    @Test
    void rejectsGroupWithNonEmptyEventTypeId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventTypeNode("x", "X", EventTypeNodeKind.GROUP, List.of(), "x", -1, List.of()));
    }
}
