package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldCondition;
import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventFilter;
import io.github.youngledo.jmcfx.domain.model.EventFilterOperator;
import io.github.youngledo.jmcfx.domain.model.EventProperty;
import io.github.youngledo.jmcfx.domain.model.EventRow;
import io.github.youngledo.jmcfx.domain.model.EventSelectionProperties;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventTypeNodeKind;
import io.github.youngledo.jmcfx.domain.model.EventTypeSelection;
import io.github.youngledo.jmcfx.domain.model.EventWindow;
import io.github.youngledo.jmcfx.domain.model.EventWindowRequest;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.EventQuerySession;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

class JmcEventQueryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsTreeFieldsWindowAndDetailsFromRecording() throws Exception {
        RecordingSummary summary = recording();
        JmcEventQueryService service = new JmcEventQueryService();

        List<EventTypeNode> tree = service.loadEventTypeTree(summary);
        EventTypeNode type = firstEventType(tree);
        List<EventFieldDescriptor> fields = service.loadFieldDescriptors(summary, type.eventTypeId());
        EventWindow window = service.loadEventWindow(summary,
                new EventWindowRequest(type.eventTypeId(), 0, 25, 0, 25,
                        fields.stream().limit(2).map(EventFieldDescriptor::id).toList(), EventFilter.empty()));
        EventDetails details = service.loadEventDetails(summary, window.rows().getFirst().id());

        assertTrue(tree.stream().map(EventTypeNode::label).toList().contains("Java Application"));
        assertFalse(fields.isEmpty());
        assertFalse(window.rows().isEmpty());
        assertTrue(window.rows().size() <= 50);
        assertNotNull(details.timing());
        assertFalse(details.properties().isEmpty());
    }

    @Test
    void loadsAvailableThreadMetadataFromDetails() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        List<EventFieldDescriptor> fields = service.loadFieldDescriptors(summary, "jdk.ThreadSleep");
        EventWindow window = service.loadEventWindow(summary,
                new EventWindowRequest("jdk.ThreadSleep", 0, 25, 0, 0,
                        fields.stream().map(EventFieldDescriptor::id).toList(), EventFilter.empty()));
        EventDetails details = service.loadEventDetails(summary, window.rows().getFirst().id());

        assertFalse(details.thread().name().isBlank());
        assertFalse(details.thread().id().isBlank());
        assertTrue(details.properties().stream()
                .anyMatch(property -> property.id().toLowerCase().contains("thread")
                        && !property.value().isBlank()));
    }

    @Test
    void sessionRejectsQueriesAfterClose() throws Exception {
        RecordingSummary summary = recording();
        JmcEventQueryService service = new JmcEventQueryService();

        EventQuerySession session = service.openSession(summary);
        session.close();

        assertThrows(JmcFxException.class, session::loadEventTypeTree);
    }

    @Test
    void eventTypeTreeContainsUniqueEventTypeIds() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<String> eventTypeIds = flattenEventTypeIds(session.loadEventTypeTree());
            assertEquals(eventTypeIds.size(), eventTypeIds.stream().distinct().count());
        }
    }

    @Test
    void eventTypeTreeUsesMetadataTopLevelGroups() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<EventTypeNode> tree = session.loadEventTypeTree();

            EventTypeNode threadSleep = findEventType(tree, "jdk.ThreadSleep");
            assertEquals("Java Application", threadSleep.categoryPath().getFirst());
        }
    }

    @Test
    void eventTypeTreeUsesJmcMetadataCategoriesFromRecording() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<EventTypeNode> tree = session.loadEventTypeTree();
            EventTypeNode threadSleep = findEventType(tree, "jdk.ThreadSleep");

            assertEquals(List.of("Java Application"), threadSleep.categoryPath());
        }
    }

    @Test
    void eventTypeTreeUsesDynamicTopLevelCategoriesFromJmcMetadata() throws Exception {
        RecordingSummary summary = customCategoryRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<EventTypeNode> tree = session.loadEventTypeTree();
            EventTypeNode customEvent = findEventType(tree, "io.github.youngledo.jmcfx.CustomCategoryEvent");

            assertEquals("Custom Platform", customEvent.categoryPath().getFirst());
            assertTrue(tree.stream().map(EventTypeNode::label).toList().contains("Custom Platform"));
        }
    }

    @Test
    void eventTypeTreeDoesNotForceUnknownCategoriesIntoUncategorized() throws Exception {
        RecordingSummary summary = customCategoryRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<EventTypeNode> tree = session.loadEventTypeTree();
            EventTypeNode customPlatform = findGroupOrNull(tree, "Custom Platform");
            EventTypeNode uncategorized = findGroupOrNull(tree, "Uncategorized");

            assertNotNull(customPlatform);
            assertEquals(1, customPlatform.count());
            assertFalse(uncategorized != null
                    && flattenLabels(uncategorized.children()).contains("Custom Feature"));
        }
    }

    @Test
    void singleEventTypeDescriptorsExcludeEventTypeAndStackTrace() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<String> descriptorIds = session.loadFieldDescriptors("jdk.ThreadSleep").stream()
                    .map(EventFieldDescriptor::id)
                    .toList();

            assertFalse(descriptorIds.contains("eventType"));
            assertFalse(descriptorIds.contains("eventStackTrace"));
            assertTrue(descriptorIds.contains("startTime"));
            assertTrue(descriptorIds.contains("duration"));
            assertTrue(descriptorIds.contains("eventThread"));
        }
    }

    @Test
    void windowRowsPopulateDescriptorDrivenStartDurationAndThreadFields() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventWindow window = session.loadEventWindow(new EventWindowRequest("jdk.ThreadSleep",
                    0, 25, 0, 0, List.of("startTime", "duration", "eventThread"), EventFilter.empty()));

            assertFalse(window.rows().isEmpty());
            assertFalse(window.rows().getFirst().fieldValues().get("startTime").isBlank());
            assertFalse(window.rows().getFirst().fieldValues().get("duration").isBlank());
            assertFalse(window.rows().getFirst().fieldValues().get("eventThread").isBlank());
        }
    }

    @Test
    void descriptorsNormalizeLocalizedMetadataLabelsToEnglish() {
        JmcEventQueryService service = new JmcEventQueryService();

        EventFieldDescriptor startTime = service.toDescriptor(new TestAttribute("startTime", "开始时间"));
        EventFieldDescriptor duration = service.toDescriptor(new TestAttribute("duration", "持续时间"));
        EventFieldDescriptor customField = service.toDescriptor(new TestAttribute("jvmUser", "JVM 用户"));

        assertEquals("Start Time", startTime.label());
        assertEquals("Duration", duration.label());
        assertEquals("JVM User", customField.label());
    }

    @Test
    void eventPropertiesUseTheSameEnglishLabelsAsDescriptors() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventWindow window = session.loadEventWindow(new EventWindowRequest("jdk.ThreadSleep",
                    0, 25, 0, 0, List.of("startTime", "duration", "eventThread"), EventFilter.empty()));
            EventDetails details = session.loadEventDetails(window.rows().getFirst().id());

            assertTrue(details.properties().stream()
                    .anyMatch(property -> property.id().equals("startTime") && property.label().equals("Start Time")));
            assertTrue(details.properties().stream()
                    .anyMatch(property -> property.id().equals("duration") && property.label().equals("Duration")));
            assertTrue(details.properties().stream()
                    .anyMatch(property -> property.id().equals("eventThread") && property.label().equals("Event Thread")));
            assertTrue(details.properties().stream()
                    .map(EventProperty::label)
                    .noneMatch(JmcEventQueryServiceTest::containsHanCharacter));
        }
    }

    @Test
    void timestampFieldAndPropertyValuesUseMillisecondDisplayFormat() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventWindow window = session.loadEventWindow(new EventWindowRequest("jdk.ThreadSleep",
                    0, 25, 0, 0, List.of("startTime", "duration", "eventThread"), EventFilter.empty()));
            EventDetails details = session.loadEventDetails(window.rows().getFirst().id());
            String rowStartTime = window.rows().getFirst().fieldValues().get("startTime");
            String propertyStartTime = details.properties().stream()
                    .filter(property -> property.id().equals("startTime"))
                    .findFirst()
                    .orElseThrow()
                    .value();

            assertTrue(rowStartTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"), rowStartTime);
            assertTrue(propertyStartTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
                    propertyStartTime);
            assertFalse(rowStartTime.contains("/"));
            assertFalse(propertyStartTime.contains("/"));
        }
    }

    @Test
    void formatsDurationNanosWithoutTicksOrBareMillis() {
        assertEquals("120 ns", JmcEventQueryService.formatDurationNanos(120));
        assertEquals("900 us", JmcEventQueryService.formatDurationNanos(900_000));
        assertEquals("42 ms", JmcEventQueryService.formatDurationNanos(42_000_000));
        assertEquals("1 s 250 ms", JmcEventQueryService.formatDurationNanos(1_250_000_000));
        assertEquals("1 min 2 s 120 ms", JmcEventQueryService.formatDurationNanos(62_120_000_000L));
    }

    @Test
    void eventPropertiesComeFromEventAttributesOnly() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventWindow window = session.loadEventWindow(new EventWindowRequest("jdk.ThreadSleep",
                    0, 25, 0, 0, List.of(), EventFilter.empty()));
            EventDetails details = session.loadEventDetails(window.rows().getFirst().id());
            List<String> propertyIds = details.properties().stream()
                    .map(EventProperty::id)
                    .toList();

            assertTrue(propertyIds.contains("startTime"));
            assertTrue(propertyIds.contains("duration"));
            assertTrue(propertyIds.contains("eventThread"));
            assertFalse(propertyIds.contains("jmcfx.eventType"));
            assertFalse(propertyIds.contains("jmcfx.startTime"));
            assertFalse(propertyIds.contains("jmcfx.duration"));
            assertFalse(propertyIds.contains("jmcfx.endTime"));
        }
    }

    @Test
    void multiTypeSelectionUsesOnlyCommonFieldDescriptors() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.group("group:java-application", "Java Application",
                    List.of("jdk.ThreadStart", "jdk.ThreadEnd"));
            List<String> descriptorIds = session.loadFieldDescriptors(selection).stream()
                    .map(EventFieldDescriptor::id)
                    .toList();

            assertTrue(descriptorIds.contains("startTime"));
            assertFalse(descriptorIds.contains("eventStackTrace"));
            assertFalse(descriptorIds.contains("eventType"));
            assertFalse(descriptorIds.contains("(eventType)"));
            assertFalse(descriptorIds.contains("(endTime)"));
        }
    }

    @Test
    void descriptorsHideParenthesizedSyntheticAttributes() throws Exception {
        RecordingSummary summary = recording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<String> descriptorIds = session.loadFieldDescriptors("jdk.ActiveRecording").stream()
                    .map(EventFieldDescriptor::id)
                    .toList();

            assertFalse(descriptorIds.contains("(endTime)"));
            assertFalse(descriptorIds.contains("(eventType)"));
            assertFalse(descriptorIds.contains("(eventTypeId)"));
        }
    }

    @Test
    void propertiesHideParenthesizedSyntheticAttributes() throws Exception {
        RecordingSummary summary = recording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventWindow window = session.loadEventWindow(new EventWindowRequest("jdk.ActiveRecording",
                    0, 25, 0, 0, List.of(), EventFilter.empty()));
            EventDetails details = session.loadEventDetails(window.rows().getFirst().id());
            List<String> propertyIds = details.properties().stream().map(EventProperty::id).toList();

            assertFalse(propertyIds.contains("(endTime)"));
            assertFalse(propertyIds.contains("(eventType)"));
            assertFalse(propertyIds.contains("(eventTypeId)"));
        }
    }

    @Test
    void formatsInfiniteTimespanAsInfinity() {
        assertEquals("∞", JmcEventQueryService.formatDurationNanos(Long.MAX_VALUE));
    }

    @Test
    void formatsMemoryQuantitiesWithReadableUnits() throws Exception {
        RecordingSummary summary = recording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            List<EventFieldDescriptor> descriptors = session.loadFieldDescriptors("jdk.ActiveRecording");
            String sizeField = descriptors.stream()
                    .map(EventFieldDescriptor::id)
                    .filter(id -> id.toLowerCase().contains("size"))
                    .findFirst()
                    .orElse("recordingMaxSize");
            EventWindow window = session.loadEventWindow(new EventWindowRequest("jdk.ActiveRecording",
                    0, 25, 0, 0, List.of(sizeField, "recordingDuration"), EventFilter.empty()));
            EventRow row = window.rows().getFirst();

            assertTrue(row.fieldValues().getOrDefault(sizeField, "").matches(".*(B|KiB|MiB|GiB|KB|MB|GB).*"),
                    row.fieldValues().toString());
            assertFalse(row.fieldValues().getOrDefault("recordingDuration", "").matches("\\d{8,}.*"),
                    row.fieldValues().toString());
        }
    }

    @Test
    void multiTypeSelectionLoadsRowsFromAllSelectedTypes() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.group("group:java-application", "Java Application",
                    List.of("jdk.ThreadStart", "jdk.ThreadEnd"));
            EventWindow window = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), EventFilter.empty()));

            assertEquals("group:java-application", window.eventTypeId());
            assertFalse(window.rows().isEmpty());
            assertEquals(new HashSet<>(selection.eventTypeIds()),
                    new HashSet<>(window.rows().stream().map(EventRow::eventTypeId).toList()));
            assertTrue(window.rows().stream().allMatch(row -> selection.eventTypeIds().contains(row.eventTypeId())));
        }
    }

    @Test
    void selectedWindowAppliesTextAndFieldFilters() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.group("group:java-application", "Java Application",
                    List.of("jdk.ThreadStart", "jdk.ThreadEnd"));
            EventFilter textFilter = new EventFilter("Thread Start", "", null, null, List.of());
            EventWindow textWindow = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), textFilter));
            EventFilter fieldFilter = new EventFilter("", "", null, null,
                    List.of(new EventFieldCondition("eventThread", EventFilterOperator.CONTAINS, "no-such-thread")));
            EventWindow emptyWindow = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), fieldFilter));

            assertFalse(textWindow.rows().isEmpty());
            assertTrue(textWindow.rows().stream().allMatch(row -> row.eventTypeId().equals("jdk.ThreadStart")));
            assertTrue(emptyWindow.rows().isEmpty());
            assertEquals(0, emptyWindow.totalCount());
        }
    }

    @Test
    void filteredRowIdsLoadDetailsForDisplayedRows() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.single("jdk.ThreadStart", "Thread Start");
            EventWindow unfiltered = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), EventFilter.empty()));
            EventRow target = unfiltered.rows().getLast();
            EventFilter filter = new EventFilter(target.fieldValues().get("startTime"), "", null, null, List.of());
            EventWindow filtered = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), filter));

            assertFalse(filtered.rows().isEmpty());
            EventRow filteredRow = filtered.rows().getFirst();
            EventDetails details = session.loadEventDetails(filteredRow.id());
            String detailsStartTime = details.properties().stream()
                    .filter(property -> property.id().equals("startTime"))
                    .findFirst()
                    .orElseThrow()
                    .value();

            assertEquals(target.id(), filteredRow.id());
            assertEquals(filteredRow.fieldValues().get("startTime"), detailsStartTime);
        }
    }

    @Test
    void invalidNumericFilterDoesNotMatchRows() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.single("jdk.ThreadStart", "Thread Start");
            EventFilter filter = new EventFilter("", "", null, null,
                    List.of(new EventFieldCondition("eventThread", EventFilterOperator.LESS_THAN, "999")));
            EventWindow window = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), filter));

            assertTrue(window.rows().isEmpty());
        }
    }

    @Test
    void nonFiniteNumericFilterDoesNotMatchRows() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.single("jdk.ThreadStart", "Thread Start");
            EventFilter filter = new EventFilter("", "", null, null,
                    List.of(new EventFieldCondition("startTime", EventFilterOperator.LESS_THAN, "NaN")));
            EventWindow window = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), filter));

            assertTrue(window.rows().isEmpty());
        }
    }

    @Test
    void multiTypeSelectionRowIdsLoadDetailsForTheSameRow() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.group("group:java-application", "Java Application",
                    List.of("jdk.ThreadStart", "jdk.ThreadEnd"));
            EventWindow window = session.loadEventWindow(new EventWindowRequest(selection,
                    0, 50, 0, 0, List.of("startTime"), EventFilter.empty()));

            for (EventRow row : window.rows()) {
                EventDetails details = session.loadEventDetails(row.id());
                String detailsStartTime = details.properties().stream()
                        .filter(property -> property.id().equals("startTime"))
                        .findFirst()
                        .orElseThrow()
                        .value();

                assertEquals(row.eventTypeId(), details.eventTypeId());
                assertEquals(row.fieldValues().get("startTime"), detailsStartTime);
            }
        }
    }

    @Test
    void selectionPropertiesSummarizeCategorySelection() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.group("group:java-application", "Java Application",
                    List.of("jdk.ThreadStart", "jdk.ThreadEnd"));
            EventSelectionProperties properties = session.loadSelectionProperties(selection);

            assertEquals("group:java-application", properties.selectionId());
            assertEquals("Java Application", properties.label());
            assertTrue(properties.eventCount() > 0);
            assertTrue(properties.properties().stream()
                    .anyMatch(property -> property.id().equals("jmcfx.selection.events")
                            && property.label().equals("Events")));
            assertTrue(properties.properties().stream().map(EventProperty::id)
                    .noneMatch(id -> id.startsWith("(") && id.endsWith(")")));
        }
    }

    @Test
    void selectionPropertiesUseRangesForDifferentQuantityValues() throws Exception {
        RecordingSummary summary = threadRecording();
        JmcEventQueryService service = new JmcEventQueryService();

        try (EventQuerySession session = service.openSession(summary)) {
            EventTypeSelection selection = EventTypeSelection.single("jdk.ThreadSleep", "Thread Sleep");
            EventSelectionProperties properties = session.loadSelectionProperties(selection);

            String startTimeRange = properties.properties().stream()
                    .filter(property -> property.id().equals("startTime"))
                    .map(EventProperty::value)
                    .findFirst()
                    .orElseThrow();

            assertTrue(startTimeRange.contains(" - "), startTimeRange);
            String[] parts = startTimeRange.split(" - ");
            assertEquals(2, parts.length);
            assertTrue(parts[0].compareTo(parts[1]) <= 0, startTimeRange);
        }
    }

    private RecordingSummary recording() throws Exception {
        Path recordingPath = tempDir.resolve("sample.jfr");
        try (Recording recording = new Recording()) {
            recording.setMaxSize(1024 * 1024);
            recording.enable("jdk.ActiveRecording").withPeriod(java.time.Duration.ofMillis(10));
            recording.enable("jdk.ThreadSleep").withThreshold(java.time.Duration.ZERO);
            recording.start();
            Thread.sleep(20);
            recording.stop();
            recording.dump(recordingPath);
        }
        return new RecordingSummary(recordingPath.toString(), recordingPath,
                recordingPath.getFileName().toString(), Instant.EPOCH, Instant.EPOCH.plusSeconds(1),
                1000, Files.size(recordingPath));
    }

    private RecordingSummary threadRecording() throws Exception {
        Path recordingPath = tempDir.resolve("thread-sleep.jfr");
        try (Recording recording = new Recording()) {
            recording.enable("jdk.ThreadSleep").withThreshold(java.time.Duration.ZERO);
            recording.enable("jdk.ThreadStart");
            recording.enable("jdk.ThreadEnd");
            recording.start();
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }, "jmcfx-test-worker");
            thread.start();
            thread.join();
            Thread.sleep(20);
            recording.stop();
            recording.dump(recordingPath);
        }
        return new RecordingSummary(recordingPath.toString(), recordingPath,
                recordingPath.getFileName().toString(), Instant.EPOCH, Instant.EPOCH.plusSeconds(1),
                1000, Files.size(recordingPath));
    }

    private RecordingSummary customCategoryRecording() throws Exception {
        Path recordingPath = tempDir.resolve("custom-category.jfr");
        try (Recording recording = new Recording()) {
            recording.enable(CustomCategoryEvent.class);
            recording.start();
            new CustomCategoryEvent().commit();
            recording.stop();
            recording.dump(recordingPath);
        }
        return new RecordingSummary(recordingPath.toString(), recordingPath,
                recordingPath.getFileName().toString(), Instant.EPOCH, Instant.EPOCH.plusSeconds(1),
                1000, Files.size(recordingPath));
    }

    private EventTypeNode firstEventType(List<EventTypeNode> nodes) {
        for (EventTypeNode node : nodes) {
            if (node.kind() == EventTypeNodeKind.EVENT_TYPE) {
                return node;
            }
            if (!node.children().isEmpty()) {
                try {
                    return firstEventType(node.children());
                } catch (java.util.NoSuchElementException exception) {
                    // Continue with the next sibling.
                }
            }
        }
        throw new java.util.NoSuchElementException();
    }

    private EventTypeNode findEventType(List<EventTypeNode> nodes, String eventTypeId) {
        for (EventTypeNode node : nodes) {
            if (node.kind() == EventTypeNodeKind.EVENT_TYPE && node.eventTypeId().equals(eventTypeId)) {
                return node;
            }
            EventTypeNode found = findEventTypeOrNull(node.children(), eventTypeId);
            if (found != null) {
                return found;
            }
        }
        throw new java.util.NoSuchElementException(eventTypeId);
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

    private EventTypeNode findGroupOrNull(List<EventTypeNode> nodes, String label) {
        return nodes.stream()
                .filter(node -> node.kind() == EventTypeNodeKind.GROUP && node.label().equals(label))
                .findFirst()
                .orElse(null);
    }

    private List<String> flattenLabels(List<EventTypeNode> nodes) {
        return nodes.stream()
                .flatMap(node -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(node.label()),
                        flattenLabels(node.children()).stream()))
                .toList();
    }

    private List<String> flattenEventTypeIds(List<EventTypeNode> nodes) {
        return nodes.stream()
                .flatMap(node -> {
                    if (node.kind() == EventTypeNodeKind.EVENT_TYPE) {
                        return java.util.stream.Stream.of(node.eventTypeId());
                    }
                    return flattenEventTypeIds(node.children()).stream();
                })
                .toList();
    }

    private static boolean containsHanCharacter(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    @Name("io.github.youngledo.jmcfx.CustomCategoryEvent")
    @Label("Custom Category Event")
    @Category({"Custom Platform", "Custom Feature"})
    static final class CustomCategoryEvent extends Event {
    }

    private record TestAttribute(String identifier, String name) implements IAttribute<String>, IAccessorKey<String> {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "description";
        }

        @Override
        public ContentType<String> getContentType() {
            return UnitLookup.PLAIN_TEXT;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public IAccessorKey<String> getKey() {
            return this;
        }

        @Override
        public <T> IMemberAccessor<String, T> getAccessor(IType<T> type) {
            return null;
        }
    }
}
