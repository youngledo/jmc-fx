package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.EventDetails;
import com.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import com.youngledo.jmcfx.domain.model.EventFilter;
import com.youngledo.jmcfx.domain.model.EventProperty;
import com.youngledo.jmcfx.domain.model.EventSelectionProperties;
import com.youngledo.jmcfx.domain.model.EventTypeNode;
import com.youngledo.jmcfx.domain.model.EventTypeNodeKind;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.EventWindow;
import com.youngledo.jmcfx.domain.model.EventWindowRequest;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.EventQuerySession;

class JmcEventQueryServiceRealJfrTest {

    @Disabled("Manual validation for local startup.jfr; do not require this file in mvn verify.")
    @Test
    void validatesLocalStartupRecording() throws Exception {
        Path path = startupRecordingPath();
        assertTrue(Files.isRegularFile(path), "startup.jfr must exist in the repository root for manual validation");
        RecordingSummary recording = new RecordingSummary("startup", path, "startup.jfr", Instant.EPOCH,
                Instant.EPOCH, 0, Files.size(path));

        try (EventQuerySession session = new JmcEventQueryService().openSession(recording)) {
            List<EventTypeNode> tree = session.loadEventTypeTree();
            List<String> topLevelLabels = tree.stream().map(EventTypeNode::label).toList();
            assertTrue(topLevelLabels.contains("Flight Recorder"), topLevelLabels.toString());
            assertTrue(topLevelLabels.contains("Java Application"), topLevelLabels.toString());
            assertTrue(topLevelLabels.contains("Java Virtual Machine"), topLevelLabels.toString());
            assertTrue(topLevelLabels.contains("Operating System"), topLevelLabels.toString());
            assertCategoryIsTopLevelWhenPresent(tree, "Java Development Kit", "Security");
            assertCategoryIsTopLevelWhenPresent(tree, "Spring Application", "Startup Step");
            assertTrue(flattenLabels(tree).contains("Flight Recorder"));
            assertTrue(flattenLabels(tree).contains("Java Application"));
            assertTrue(flattenLabels(tree).contains("Java Virtual Machine"));
            assertTrue(flattenLabels(tree).contains("Operating System"));
            assertTrue(tree.stream().flatMap(node -> node.children().stream())
                    .anyMatch(node -> node.kind() == EventTypeNodeKind.GROUP));

            EventTypeNode cpuLoad = findEventTypeOrNull(tree, "jdk.CPULoad");
            if (cpuLoad != null) {
                assertTrue(cpuLoad.categoryPath().size() >= 2, cpuLoad.categoryPath().toString());
                assertEquals("Operating System", cpuLoad.categoryPath().getFirst());
            }

            List<EventTypeNode> leaves = leaves(tree);
            Set<String> uniqueIds = new HashSet<>();
            for (EventTypeNode leaf : leaves) {
                assertTrue(uniqueIds.add(leaf.eventTypeId()), "duplicate event type id: " + leaf.eventTypeId());
            }

            assertTrue(leaves.stream().anyMatch(type -> type.eventTypeId().equals("jdk.ObjectAllocationInNewTLAB")));
            EventTypeNode flightRecorder = findGroup(tree, "Flight Recorder");
            EventTypeSelection flightRecorderSelection = EventTypeSelection.group(flightRecorder.id(),
                    flightRecorder.label(), leaves(List.of(flightRecorder)).stream()
                            .map(EventTypeNode::eventTypeId)
                            .toList());
            List<EventFieldDescriptor> flightRecorderFields = session.loadFieldDescriptors(flightRecorderSelection);
            assertTrue(flightRecorderFields.stream().map(EventFieldDescriptor::id)
                    .noneMatch(id -> id.startsWith("(") && id.endsWith(")")));
            EventWindow flightRecorderWindow = session.loadEventWindow(new EventWindowRequest(flightRecorderSelection,
                    0, 100, 0, 0, flightRecorderFields.stream().map(EventFieldDescriptor::id).toList(),
                    EventFilter.empty()));
            assertFalse(flightRecorderWindow.rows().isEmpty());
            assertEquals(flightRecorderSelection.id(), flightRecorderWindow.eventTypeId());
            assertTrue(flightRecorderWindow.rows().stream()
                    .allMatch(row -> flightRecorderSelection.eventTypeIds().contains(row.eventTypeId())));
            EventSelectionProperties flightRecorderProperties = session.loadSelectionProperties(flightRecorderSelection);
            assertTrue(flightRecorderProperties.properties().stream().map(EventProperty::id)
                    .noneMatch(id -> id.startsWith("(") && id.endsWith(")")));
            assertTrue(flightRecorderProperties.properties().stream()
                    .anyMatch(property -> property.id().equals("jmcfx.selection.events")));

            EventWindow first = session.loadEventWindow(new EventWindowRequest("jdk.ObjectAllocationInNewTLAB",
                    0, 200, 0, 200, List.of(), EventFilter.empty()));
            EventWindow middle = session.loadEventWindow(new EventWindowRequest("jdk.ObjectAllocationInNewTLAB",
                    10_000, 200, 50, 200, List.of(), EventFilter.empty()));

            List<EventFieldDescriptor> fields = session.loadFieldDescriptors(leaves.getFirst().eventTypeId());
            List<String> fieldIds = fields.stream().map(EventFieldDescriptor::id).toList();
            assertFalse(fieldIds.contains("eventType"));
            assertFalse(fieldIds.contains("eventStackTrace"));
            assertTrue(fields.stream().map(EventFieldDescriptor::label).noneMatch(this::containsHanCharacter));

            List<EventFieldDescriptor> allocationFields = session.loadFieldDescriptors("jdk.ObjectAllocationInNewTLAB");
            List<String> allocationFieldIds = allocationFields.stream().map(EventFieldDescriptor::id).toList();
            EventWindow allocationWindow = session.loadEventWindow(new EventWindowRequest("jdk.ObjectAllocationInNewTLAB",
                    0, 25, 0, 0, allocationFieldIds, EventFilter.empty()));
            assertFalse(allocationWindow.rows().isEmpty());

            EventDetails allocationDetails = session.loadEventDetails(allocationWindow.rows().getFirst().id());
            assertTrue(allocationDetails.properties().stream().map(EventProperty::label).noneMatch(this::containsHanCharacter));
            if (allocationFieldIds.contains("startTime")) {
                String startTimeValue = allocationWindow.rows().getFirst().fieldValues().get("startTime");
                assertNotNull(startTimeValue);
                assertTrue(startTimeValue.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
                        startTimeValue);
            }

            assertFalse(first.rows().isEmpty());
            assertFalse(middle.rows().isEmpty());
            assertTrue(first.rows().size() <= 400);
            assertTrue(middle.rows().size() <= 450);
        }
    }

    private List<EventTypeNode> leaves(List<EventTypeNode> nodes) {
        ArrayDeque<EventTypeNode> queue = new ArrayDeque<>(nodes);
        java.util.ArrayList<EventTypeNode> leaves = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            EventTypeNode node = queue.removeFirst();
            if (node.kind() == EventTypeNodeKind.EVENT_TYPE) {
                leaves.add(node);
            } else {
                queue.addAll(node.children());
            }
        }
        return List.copyOf(leaves);
    }

    private boolean categoryContainsGroup(List<EventTypeNode> nodes, String category, String childLabel) {
        return nodes.stream()
                .filter(node -> node.kind() == EventTypeNodeKind.GROUP && node.label().equals(category))
                .flatMap(node -> node.children().stream())
                .anyMatch(node -> node.kind() == EventTypeNodeKind.GROUP && node.label().equals(childLabel));
    }

    private EventTypeNode findGroup(List<EventTypeNode> nodes, String label) {
        return nodes.stream()
                .filter(node -> node.kind() == EventTypeNodeKind.GROUP && node.label().equals(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing group: " + label));
    }

    private void assertCategoryIsTopLevelWhenPresent(List<EventTypeNode> nodes, String category, String childLabel) {
        if (flattenLabels(nodes).contains(childLabel)) {
            List<String> topLevelLabels = nodes.stream().map(EventTypeNode::label).toList();
            assertTrue(topLevelLabels.contains(category), topLevelLabels.toString());
        }
        assertFalse(categoryContainsGroup(nodes, "Uncategorized", childLabel));
    }

    private boolean containsHanCharacter(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private List<String> flattenLabels(List<EventTypeNode> nodes) {
        return nodes.stream()
                .flatMap(node -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(node.label()),
                        flattenLabels(node.children()).stream()))
                .toList();
    }

    private EventTypeNode findEventTypeOrNull(List<EventTypeNode> nodes, String eventTypeId) {
        for (EventTypeNode node : nodes) {
            if (node.kind() == EventTypeNodeKind.EVENT_TYPE && node.eventTypeId().equals(eventTypeId)) {
                return node;
            }
            EventTypeNode found = findEventTypeOrNull(node.children(), eventTypeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Path startupRecordingPath() {
        String configuredPath = System.getProperty("jmcfx.realJfr", "");
        if (!configuredPath.isBlank()) {
            return Path.of(configuredPath);
        }
        Path modulePath = Path.of("startup.jfr");
        if (Files.isRegularFile(modulePath)) {
            return modulePath;
        }
        return Path.of("..", "startup.jfr");
    }
}
