package com.youngledo.jmcfx.adapter.jmc;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;

import com.youngledo.jmcfx.domain.model.EventDetails;
import com.youngledo.jmcfx.domain.model.EventFieldCondition;
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
import com.youngledo.jmcfx.domain.service.JmcFxException;

/// JMC-backed implementation of windowed event browser queries.
///
/// This adapter converts OpenJDK JMC item collections into UI-neutral domain
/// records so JavaFX code never depends on JMC APIs directly.
public class JmcEventQueryService implements EventQueryService {

    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Map<String, String> ATTRIBUTE_LABELS = Map.ofEntries(
            Map.entry("startTime", "Start Time"),
            Map.entry("endTime", "End Time"),
            Map.entry("duration", "Duration"),
            Map.entry("eventThread", "Event Thread"),
            Map.entry("eventStackTrace", "Event Stack Trace"),
            Map.entry("eventType", "Event Type"),
            Map.entry("jvmUser", "JVM User"),
            Map.entry("jvmSystem", "JVM System"),
            Map.entry("machineTotal", "Machine Total"),
            Map.entry("recordingDestination", "Destination"),
            Map.entry("recordingDuration", "Recording Duration"),
            Map.entry("recordingMaxAge", "Max Age"),
            Map.entry("recordingMaxSize", "Max Size"),
            Map.entry("recordingStart", "Start Time"),
            Map.entry("threadId", "Thread Id"));

    @Override
    public EventQuerySession openSession(RecordingSummary recording) {
        return new JmcEventQuerySession(recording, load(recording));
    }

    private List<EventTypeNode> loadEventTypeTree(RecordingEvents recordingEvents) {
        CategoryGroupBuilder root = new CategoryGroupBuilder(List.of());
        eventTypeMetadata(recordingEvents).stream()
                .sorted(Comparator.comparing(metadata -> blankToDefault(metadata.type().getName(),
                        metadata.type().getIdentifier())))
                .forEach(metadata -> root.add(metadata.categoryPath(), EventTypeNode.eventType(
                        metadata.type().getIdentifier(),
                        blankToDefault(metadata.type().getName(), metadata.type().getIdentifier()),
                        metadata.categoryPath(), metadata.count())));
        return root.children();
    }

    private List<EventTypeMetadata> eventTypeMetadata(RecordingEvents recordingEvents) {
        Map<String, Long> itemCountByType = recordingEvents.events().stream()
                .filter(IItemIterable::hasItems)
                .collect(Collectors.toMap(iterable -> iterable.getType().getIdentifier(), IItemIterable::getItemCount,
                        Long::sum,
                        LinkedHashMap::new));
        return Arrays.stream(recordingEvents.eventArrays().getArrays())
                .map(eventArray -> {
                    Long count = itemCountByType.remove(eventArray.getType().getIdentifier());
                    if (count == null) {
                        return null;
                    }
                    return new EventTypeMetadata(eventArray.getType(), categoryPathFromJmc(eventArray.getTypeCategory()),
                            count);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<String> categoryPathFromJmc(String[] category) {
        if (category == null || category.length == 0) {
            return List.of("Uncategorized");
        }
        List<String> path = Arrays.stream(category)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        return path.isEmpty() ? List.of("Uncategorized") : path;
    }

    private List<EventFieldDescriptor> loadFieldDescriptors(IItemCollection events, EventTypeSelection selection) {
        List<IType<IItem>> selectedTypes = typesFor(events, selection);
        if (selectedTypes.isEmpty()) {
            return List.of();
        }
        Set<String> commonAttributeIds = selectedTypes.getFirst().getAttributes().stream()
                .map(IAttribute::getIdentifier)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (IType<IItem> type : selectedTypes.subList(1, selectedTypes.size())) {
            Set<String> typeAttributeIds = type.getAttributes().stream()
                    .map(IAttribute::getIdentifier)
                    .collect(Collectors.toSet());
            commonAttributeIds.retainAll(typeAttributeIds);
        }
        return selectedTypes.getFirst().getAttributes().stream()
                .filter(attribute -> commonAttributeIds.contains(attribute.getIdentifier()))
                .filter(this::displayableAttribute)
                .map(this::toDescriptor)
                .toList();
    }

    private EventWindow loadEventWindow(IItemCollection events, EventWindowRequest request) {
        IItemCollection filtered = filterBySelection(events, request.selection());
        WindowItems windowItems = windowItems(filtered, request);
        Set<String> requestedFields = new HashSet<>(request.columnFieldIds());
        List<EventRow> rows = new ArrayList<>(windowItems.items().size());
        for (WindowItem item : windowItems.items()) {
            rows.add(toRow(item.eventTypeId(), item.typeIndex(), item.item(), requestedFields));
        }
        EventLoadState state = rows.isEmpty() ? EventLoadState.EMPTY : EventLoadState.COMPLETE;
        return new EventWindow(request.eventTypeId(), request.loadStartRow(), rows, windowItems.totalCount(), true,
                state);
    }

    private EventSelectionProperties loadSelectionProperties(IItemCollection events, EventTypeSelection selection) {
        IItemCollection filtered = filterBySelection(events, selection);
        long eventCount = totalCount(filtered);
        List<EventProperty> properties = new ArrayList<>(selectionProperties(filtered, selection));
        properties.add(new EventProperty("jmcfx.selection.events", "Events", Long.toString(eventCount), "", ""));
        return new EventSelectionProperties(selection.id(), selection.label(), eventCount, properties);
    }

    private List<EventProperty> selectionProperties(IItemCollection events, EventTypeSelection selection) {
        List<IType<IItem>> selectedTypes = typesFor(events, selection);
        if (selectedTypes.isEmpty()) {
            return List.of();
        }
        Set<String> commonAttributeIds = selectedTypes.getFirst().getAttributes().stream()
                .map(IAttribute::getIdentifier)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (IType<IItem> type : selectedTypes.subList(1, selectedTypes.size())) {
            Set<String> typeAttributeIds = type.getAttributes().stream()
                    .map(IAttribute::getIdentifier)
                    .collect(Collectors.toSet());
            commonAttributeIds.retainAll(typeAttributeIds);
        }
        return selectedTypes.getFirst().getAttributes().stream()
                .filter(attribute -> commonAttributeIds.contains(attribute.getIdentifier()))
                .filter(this::displayableAttribute)
                .map(attribute -> selectionProperty(attribute, events))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private EventProperty selectionProperty(IAttribute<?> attribute, IItemCollection events) {
        if (valueType(attribute) == EventValueType.TIMESTAMP
                || UnitLookup.TIMESPAN.equals(attribute.getContentType())) {
            return quantitySelectionProperty(attribute, events);
        }
        List<String> values = events.stream()
                .flatMap(IItemIterable::stream)
                .map(item -> stringValue(attribute, readRaw(attribute, item)))
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(11)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        String value = values.size() == 1 ? values.getFirst()
                : values.size() > 10 ? "Too many values"
                : values.getFirst() + " - " + values.getLast();
        return new EventProperty(attribute.getIdentifier(), labelFor(attribute), value, unit(attribute),
                englishDescription(attribute));
    }

    private EventProperty quantitySelectionProperty(IAttribute<?> attribute, IItemCollection events) {
        IQuantity min = null;
        IQuantity max = null;
        try (Stream<IItem> items = events.stream().flatMap(IItemIterable::stream)) {
            Iterator<IItem> iterator = items.iterator();
            while (iterator.hasNext()) {
                Object value = readRaw(attribute, iterator.next());
                if (!(value instanceof IQuantity quantity)) {
                    continue;
                }
                if (min == null || quantitySortValue(attribute, quantity) < quantitySortValue(attribute, min)) {
                    min = quantity;
                }
                if (max == null || quantitySortValue(attribute, quantity) > quantitySortValue(attribute, max)) {
                    max = quantity;
                }
            }
        }
        if (min == null || max == null) {
            return null;
        }
        String minText = stringValue(attribute, min);
        String maxText = stringValue(attribute, max);
        String value = minText.equals(maxText) ? minText : minText + " - " + maxText;
        return value.isBlank() ? null : new EventProperty(attribute.getIdentifier(), labelFor(attribute), value,
                unit(attribute), englishDescription(attribute));
    }

    private long quantitySortValue(IAttribute<?> attribute, IQuantity quantity) {
        if (valueType(attribute) == EventValueType.TIMESTAMP) {
            return quantity.clampedLongValueIn(UnitLookup.EPOCH_NS);
        }
        return quantity.clampedLongValueIn(UnitLookup.NANOSECOND);
    }

    private EventDetails loadEventDetails(IItemCollection events, String eventId) {
        ParsedEventId parsed = parseEventId(eventId);
        IItem item = events.apply(ItemFilters.type(parsed.eventTypeId())).stream()
                .flatMap(IItemIterable::stream)
                .skip(parsed.index())
                .findFirst()
                .orElseThrow(() -> new JmcFxException("Event not found: " + eventId));
        return toDetails(eventId, parsed.eventTypeId(), item);
    }

    private EventTypeNode group(List<String> categoryPath, List<EventTypeNode> children) {
        return EventTypeNode.group(groupId(categoryPath), categoryPath.getLast(), categoryPath, children);
    }

    private String groupId(List<String> categoryPath) {
        return categoryPath.stream()
                .map(category -> category.toLowerCase().replace(' ', '-'))
                .collect(Collectors.joining("/"));
    }

    private IItemCollection filterBySelection(IItemCollection events, EventTypeSelection selection) {
        return events.apply(ItemFilters.type(new HashSet<>(selection.eventTypeIds())));
    }

    private WindowItems windowItems(IItemCollection events, EventWindowRequest request) {
        List<WindowItem> items = new ArrayList<>(request.loadRowCount());
        Map<String, Long> nextIndexByType = new HashMap<>();
        long matchedCount = 0;
        try (Stream<IItem> stream = events.stream().flatMap(IItemIterable::stream)) {
            Iterator<IItem> iterator = stream.iterator();
            while (iterator.hasNext()) {
                IItem item = iterator.next();
                String eventTypeId = item.getType().getIdentifier();
                long typeIndex = nextIndexByType.merge(eventTypeId, 1L, Long::sum) - 1;
                if (!matchesEventFilter(item, request.filter())) {
                    continue;
                }
                if (matchedCount >= request.loadStartRow() && items.size() < request.loadRowCount()) {
                    items.add(new WindowItem(item, eventTypeId, typeIndex));
                }
                matchedCount++;
            }
        }
        return new WindowItems(List.copyOf(items), matchedCount);
    }

    private boolean matchesEventFilter(IItem item, EventFilter filter) {
        if (filter == null || !filter.active()) {
            return true;
        }
        if (!matchesTimeRange(item, filter)) {
            return false;
        }
        if (!filter.thread().isBlank()) {
            EventThreadInfo thread = threadInfo(item);
            if (!normalize(thread.name() + " " + thread.id()).contains(normalize(filter.thread()))) {
                return false;
            }
        }
        if (!filter.text().isBlank() && !matchesText(item, filter.text())) {
            return false;
        }
        return filter.fieldConditions().stream()
                .allMatch(condition -> matchesFieldCondition(item, condition));
    }

    private boolean matchesTimeRange(IItem item, EventFilter filter) {
        Instant startTime = quantityToInstant(read(JfrAttributes.START_TIME, item));
        if (filter.startTime() != null && startTime.isBefore(filter.startTime())) {
            return false;
        }
        return filter.endTime() == null || !startTime.isAfter(filter.endTime());
    }

    private boolean matchesText(IItem item, String text) {
        String wanted = normalize(text);
        if (normalize(item.getType().getIdentifier() + " " + item.getType().getName()).contains(wanted)) {
            return true;
        }
        return item.getType().getAttributes().stream()
                .filter(this::displayableAttribute)
                .map(attribute -> stringValue(attribute, readRaw(attribute, item)))
                .anyMatch(value -> normalize(value).contains(wanted));
    }

    private boolean matchesFieldCondition(IItem item, EventFieldCondition condition) {
        Optional<IAttribute<?>> attribute = item.getType().getAttributes().stream()
                .filter(candidate -> condition.fieldId().equals(candidate.getIdentifier()))
                .findFirst();
        if (attribute.isEmpty()) {
            return false;
        }
        Object rawValue = readRaw(attribute.get(), item);
        String actual = stringValue(attribute.get(), rawValue);
        String expected = condition.value();
        return switch (condition.operator()) {
            case CONTAINS -> normalize(actual).contains(normalize(expected));
            case EQUALS -> actual.equalsIgnoreCase(expected);
            case NOT_EQUALS -> !actual.equalsIgnoreCase(expected);
            case IS_TRUE -> booleanValue(rawValue).orElse(Boolean.parseBoolean(actual));
            case IS_FALSE -> !booleanValue(rawValue).orElse(Boolean.parseBoolean(actual));
            case GREATER_THAN -> compareNumeric(rawValue, expected).map(comparison -> comparison > 0).orElse(false);
            case GREATER_THAN_OR_EQUAL -> compareNumeric(rawValue, expected).map(comparison -> comparison >= 0).orElse(false);
            case LESS_THAN -> compareNumeric(rawValue, expected).map(comparison -> comparison < 0).orElse(false);
            case LESS_THAN_OR_EQUAL -> compareNumeric(rawValue, expected).map(comparison -> comparison <= 0).orElse(false);
        };
    }

    private Optional<Integer> compareNumeric(Object rawValue, String expected) {
        Optional<Double> actual = numericValue(rawValue);
        Optional<Double> wanted = parseDouble(expected);
        if (actual.isEmpty() || wanted.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Double.compare(actual.get(), wanted.get()));
    }

    private Optional<Double> numericValue(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.doubleValue());
        }
        if (value instanceof IQuantity quantity) {
            return Optional.of(quantity.doubleValue());
        }
        return parseDouble(stringValue(value));
    }

    private Optional<Double> parseDouble(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private List<IType<IItem>> typesFor(IItemCollection events, EventTypeSelection selection) {
        Set<String> wanted = new HashSet<>(selection.eventTypeIds());
        return events.stream()
                .map(IItemIterable::getType)
                .filter(type -> wanted.contains(type.getIdentifier()))
                .distinct()
                .toList();
    }

    private boolean displayableAttribute(IAttribute<?> attribute) {
        String identifier = attribute.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        return !attribute.equals(JfrAttributes.EVENT_TYPE)
                && !attribute.equals(JfrAttributes.EVENT_TYPE_ID)
                && !attribute.equals(JfrAttributes.EVENT_STACKTRACE)
                && !attribute.equals(JfrAttributes.END_TIME)
                && (!identifier.startsWith("(") || !identifier.endsWith(")"));
    }

    EventFieldDescriptor toDescriptor(IAttribute<?> attribute) {
        String id = attribute.getIdentifier();
        return new EventFieldDescriptor(id, labelFor(attribute),
                englishDescription(attribute), valueType(attribute), unit(attribute), true, true, true);
    }

    private EventRow toRow(String eventTypeId, long typeIndex, IItem item, Set<String> requestedFields) {
        Instant startTime = quantityToInstant(read(JfrAttributes.START_TIME, item));
        IQuantity duration = read(JfrAttributes.DURATION, item);
        String durationText = durationText(duration);
        long durationNanos = duration == null ? 0 : duration.clampedLongValueIn(UnitLookup.NANOSECOND);
        String threadName = stringValue(read(JdkAttributes.EVENT_THREAD_NAME, item));
        Map<String, String> values = fieldValues(item, requestedFields);
        return new EventRow(eventTypeId + "#" + typeIndex, eventTypeId, startTime, formatInstant(startTime),
                durationNanos, durationText, threadName, values);
    }

    private EventDetails toDetails(String eventId, String eventTypeId, IItem item) {
        Instant startTime = quantityToInstant(read(JfrAttributes.START_TIME, item));
        IQuantity duration = read(JfrAttributes.DURATION, item);
        String durationText = durationText(duration);
        long durationNanos = duration == null ? 0 : duration.clampedLongValueIn(UnitLookup.NANOSECOND);
        Instant endTime = quantityToInstant(read(JfrAttributes.END_TIME, item));
        EventTiming timing = new EventTiming(startTime, endTime, durationNanos, durationText, "");
        return new EventDetails(eventId, eventTypeId, properties(item), timing, threadInfo(item), stackTrace(item));
    }

    private Map<String, String> fieldValues(IItem item, Set<String> requestedFields) {
        return item.getType().getAttributes().stream()
                .filter(this::displayableAttribute)
                .filter(attribute -> requestedFields.contains(attribute.getIdentifier()))
                .collect(Collectors.toMap(IAttribute::getIdentifier,
                        attribute -> stringValue(attribute, readRaw(attribute, item)),
                        (left, right) -> right, LinkedHashMap::new));
    }

    private List<EventProperty> properties(IItem item) {
        return item.getType().getAttributes().stream()
                .filter(this::displayableAttribute)
                .map(attribute -> new EventProperty(attribute.getIdentifier(), labelFor(attribute),
                        stringValue(attribute, readRaw(attribute, item)), unit(attribute),
                        englishDescription(attribute)))
                .toList();
    }

    private String labelFor(IAttribute<?> attribute) {
        String identifier = attribute.getIdentifier();
        return ATTRIBUTE_LABELS.getOrDefault(identifier, titleFromIdentifier(identifier));
    }

    private String titleFromIdentifier(String identifier) {
        String title = blankToDefault(identifier, "")
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        return Arrays.stream(title.split("\\s+"))
                .filter(word -> !word.isBlank())
                .map(this::capitalizeWord)
                .collect(Collectors.joining(" "));
    }

    private String capitalizeWord(String word) {
        if (word.isBlank()) {
            return "";
        }
        if (word.chars().allMatch(Character::isUpperCase)) {
            return word;
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    private EventThreadInfo threadInfo(IItem item) {
        IMCThread thread = read(JfrAttributes.EVENT_THREAD, item);
        String name = firstPresent(thread == null ? "" : thread.getThreadName(),
                stringValue(read(JdkAttributes.EVENT_THREAD_NAME, item)), threadAttributeValue(item, "name"));
        String id = firstPresent(thread == null || thread.getThreadId() == null ? "" : thread.getThreadId().toString(),
                threadAttributeValue(item, "javaThreadId"), threadAttributeValue(item, "osThreadId"),
                threadAttributeValue(item, "threadId"));
        boolean virtual = threadVirtual(item).orElse(false);
        return new EventThreadInfo(name, id, virtual);
    }

    private String threadAttributeValue(IItem item, String wanted) {
        String wantedText = normalize(wanted);
        return item.getType().getAttributes().stream()
                .filter(attribute -> {
                    String text = normalize(attribute.getIdentifier() + " " + attribute.getName());
                    return text.contains("thread") && text.contains(wantedText);
                })
                .map(attribute -> stringValue(readRaw(attribute, item)))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private Optional<Boolean> threadVirtual(IItem item) {
        return item.getType().getAttributes().stream()
                .filter(attribute -> normalize(attribute.getIdentifier() + " " + attribute.getName()).contains("virtual"))
                .map(attribute -> readRaw(attribute, item))
                .flatMap(value -> booleanValue(value).stream())
                .findFirst();
    }

    private Optional<Boolean> booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
        }
        String text = stringValue(value);
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Optional.of(Boolean.parseBoolean(text));
        }
        return Optional.empty();
    }

    private List<EventStackFrame> stackTrace(IItem item) {
        try {
            Object stackTrace = read(JfrAttributes.EVENT_STACKTRACE, item);
            if (stackTrace instanceof IMCStackTrace jmcStackTrace) {
                return jmcStackTrace.getFrames().stream()
                        .map(this::stackFrame)
                        .toList();
            }
            return fallbackStackTrace(item, stackTrace);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private EventStackFrame stackFrame(IMCFrame frame) {
        IMCMethod method = frame.getMethod();
        IMCType type = method == null ? null : method.getType();
        String typeName = type == null ? "" : firstPresent(type.getFullName(), type.getTypeName());
        String methodName = method == null ? "" : blankToDefault(method.getMethodName(), "");
        int lineNumber = frame.getFrameLineNumber() == null ? -1 : frame.getFrameLineNumber();
        return new EventStackFrame(typeName, methodName, "", lineNumber);
    }

    private List<EventStackFrame> fallbackStackTrace(IItem item, Object stackTrace) {
        String text = stringValue(stackTrace);
        if (text.isBlank()) {
            text = item.getType().getAttributes().stream()
                    .filter(attribute -> normalize(attribute.getIdentifier() + " " + attribute.getName()).contains("stack"))
                    .map(attribute -> stringValue(readRaw(attribute, item)))
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse("");
        }
        return text.isBlank() ? List.of() : List.of(new EventStackFrame("", text, "", -1));
    }

    private RecordingEvents load(RecordingSummary recording) {
        JmcRecordingDataCache.RecordingData data = JmcRecordingDataCache.SHARED.recording(recording);
        return new RecordingEvents(data.events(), data.eventArrays());
    }

    private long totalCount(IItemCollection events) {
        return events.stream()
                .mapToLong(IItemIterable::getItemCount)
                .sum();
    }

    private ParsedEventId parseEventId(String eventId) {
        int separator = eventId.lastIndexOf('#');
        if (separator <= 0 || separator == eventId.length() - 1) {
            throw new JmcFxException("Invalid event id: " + eventId);
        }
        try {
            return new ParsedEventId(eventId.substring(0, separator), Long.parseLong(eventId.substring(separator + 1)));
        } catch (NumberFormatException exception) {
            throw new JmcFxException("Invalid event id: " + eventId, exception);
        }
    }

    static String formatDurationNanos(long nanos) {
        if (nanos == Long.MAX_VALUE) {
            return "∞";
        }
        if (nanos == Long.MIN_VALUE) {
            return "";
        }
        if (nanos < 1_000) {
            return nanos + " ns";
        }
        if (nanos < 1_000_000) {
            return nanos / 1_000 + " us";
        }
        if (nanos < 1_000_000_000) {
            return nanos / 1_000_000 + " ms";
        }
        long minutes = nanos / 60_000_000_000L;
        long remainder = nanos % 60_000_000_000L;
        long seconds = remainder / 1_000_000_000L;
        long millis = (remainder % 1_000_000_000L) / 1_000_000L;
        if (minutes > 0) {
            return minutes + " min " + seconds + " s" + (millis > 0 ? " " + millis + " ms" : "");
        }
        return seconds + " s" + (millis > 0 ? " " + millis + " ms" : "");
    }

    private String durationText(IQuantity duration) {
        if (duration == null) {
            return "";
        }
        return formatDurationNanos(duration.clampedLongValueIn(UnitLookup.NANOSECOND));
    }

    private EventValueType valueType(IAttribute<?> attribute) {
        String contentType = attribute.getKey().getContentType().getIdentifier().toLowerCase();
        if (contentType.contains("timestamp")) {
            return EventValueType.TIMESTAMP;
        }
        if (contentType.contains("timespan")) {
            return EventValueType.DURATION;
        }
        if (contentType.contains("number") || contentType.contains("memory") || contentType.contains("percentage")) {
            return EventValueType.NUMBER;
        }
        if (contentType.contains("boolean")) {
            return EventValueType.BOOLEAN;
        }
        return EventValueType.TEXT;
    }

    @SuppressWarnings("unchecked")
    private <T> T read(IAttribute<T> attribute, IItem item) {
        IMemberAccessor<T, IItem> accessor = (IMemberAccessor<T, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    @SuppressWarnings("unchecked")
    private Object readRaw(IAttribute<?> attribute, IItem item) {
        IMemberAccessor<Object, IItem> accessor = (IMemberAccessor<Object, IItem>) item.getType()
                .getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    private Instant quantityToInstant(IQuantity quantity) {
        if (quantity == null) {
            return Instant.EPOCH;
        }
        return UnitLookup.toDate(quantity).toInstant();
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "" : EVENT_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(instant);
    }

    private String stringValue(IAttribute<?> attribute, Object value) {
        if (value instanceof IQuantity quantity) {
            if (valueType(attribute) == EventValueType.TIMESTAMP) {
                return formatInstant(quantityToInstant(quantity));
            }
            if (UnitLookup.TIMESPAN.equals(quantity.getType())) {
                return formatDurationNanos(quantity.clampedLongValueIn(UnitLookup.NANOSECOND));
            }
            return quantity.displayUsing(IDisplayable.AUTO);
        }
        return stringValue(value);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof IQuantity quantity) {
            if (UnitLookup.TIMESPAN.equals(quantity.getType())) {
                return formatDurationNanos(quantity.clampedLongValueIn(UnitLookup.NANOSECOND));
            }
            return quantity.interactiveFormat();
        }
        return value.toString();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    }

    private String unit(IAttribute<?> attribute) {
        String name = attribute.getContentType().getName();
        return containsHanCharacter(name) ? "" : name;
    }

    private String englishDescription(IAttribute<?> attribute) {
        String description = blankToDefault(attribute.getDescription(), "");
        return containsHanCharacter(description) ? "" : description;
    }

    private boolean containsHanCharacter(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private final class JmcEventQuerySession implements EventQuerySession {
        private final RecordingSummary recording;
        private final RecordingEvents recordingEvents;
        private boolean closed;

        private JmcEventQuerySession(RecordingSummary recording, RecordingEvents recordingEvents) {
            this.recording = recording;
            this.recordingEvents = recordingEvents;
        }

        @Override
        public List<EventTypeNode> loadEventTypeTree() {
            ensureOpen();
            return JmcEventQueryService.this.loadEventTypeTree(recordingEvents);
        }

        @Override
        public List<EventFieldDescriptor> loadFieldDescriptors(EventTypeSelection selection) {
            ensureOpen();
            return JmcEventQueryService.this.loadFieldDescriptors(recordingEvents.events(), selection);
        }

        @Override
        public EventWindow loadEventWindow(EventWindowRequest request) {
            ensureOpen();
            return JmcEventQueryService.this.loadEventWindow(recordingEvents.events(), request);
        }

        @Override
        public EventSelectionProperties loadSelectionProperties(EventTypeSelection selection) {
            ensureOpen();
            return JmcEventQueryService.this.loadSelectionProperties(recordingEvents.events(), selection);
        }

        @Override
        public EventDetails loadEventDetails(String eventId) {
            ensureOpen();
            return JmcEventQueryService.this.loadEventDetails(recordingEvents.events(), eventId);
        }

        @Override
        public void close() {
            closed = true;
        }

        private void ensureOpen() {
            if (closed) {
                throw new JmcFxException("Event query session is closed: " + recording.path());
            }
        }
    }

    private final class CategoryGroupBuilder {
        private final List<String> categoryPath;
        private final Map<String, CategoryGroupBuilder> groups = new LinkedHashMap<>();
        private final List<EventTypeNode> leaves = new ArrayList<>();

        private CategoryGroupBuilder(List<String> categoryPath) {
            this.categoryPath = List.copyOf(categoryPath);
        }

        private void add(List<String> leafCategoryPath, EventTypeNode leaf) {
            if (categoryPath.size() >= leafCategoryPath.size()) {
                leaves.add(leaf);
                return;
            }
            String childLabel = leafCategoryPath.get(categoryPath.size());
            List<String> childPath = leafCategoryPath.subList(0, categoryPath.size() + 1);
            groups.computeIfAbsent(childLabel, ignored -> new CategoryGroupBuilder(childPath))
                    .add(leafCategoryPath, leaf);
        }

        private List<EventTypeNode> children() {
            List<EventTypeNode> children = new ArrayList<>();
            groups.values().stream()
                    .map(CategoryGroupBuilder::build)
                    .sorted(Comparator.comparing(EventTypeNode::label))
                    .forEach(children::add);
            leaves.stream()
                    .sorted(Comparator.comparing(EventTypeNode::label))
                    .forEach(children::add);
            return List.copyOf(children);
        }

        private EventTypeNode build() {
            return group(categoryPath, children());
        }
    }

    private record EventTypeMetadata(IType<IItem> type, List<String> categoryPath, long count) {
    }

    private record RecordingEvents(IItemCollection events, EventArrays eventArrays) {
    }

    private record ParsedEventId(String eventTypeId, long index) {
    }

    private record WindowItems(List<WindowItem> items, long totalCount) {
    }

    private record WindowItem(IItem item, String eventTypeId, long typeIndex) {
    }
}
