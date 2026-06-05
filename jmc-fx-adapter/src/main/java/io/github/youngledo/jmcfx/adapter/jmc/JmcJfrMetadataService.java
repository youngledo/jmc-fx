package io.github.youngledo.jmcfx.adapter.jmc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;

import io.github.youngledo.jmcfx.domain.model.EventValueType;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataField;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.JfrMetadataService;

/// JMC-backed JFR metadata service.
///
/// This adapter keeps OpenJDK JMC item/type APIs inside the adapter layer and
/// returns UI-neutral domain records to the JavaFX modules.
public class JmcJfrMetadataService implements JfrMetadataService {

    @Override
    public JfrMetadataReport loadMetadata(RecordingSummary recording) {
        Objects.requireNonNull(recording, "recording");
        JmcRecordingDataCache.RecordingData data = JmcRecordingDataCache.SHARED.recording(recording);
        Map<String, Long> countByType = data.events().stream()
                .filter(IItemIterable::hasItems)
                .collect(Collectors.toMap(iterable -> iterable.getType().getIdentifier(),
                        IItemIterable::getItemCount, Long::sum, LinkedHashMap::new));
        Map<String, IType<IItem>> typeById = data.events().stream()
                .collect(Collectors.toMap(iterable -> iterable.getType().getIdentifier(),
                        IItemIterable::getType, (left, right) -> left, LinkedHashMap::new));
        EventArrays arrays = data.eventArrays();
        List<JfrMetadataEventType> eventTypes = Arrays.stream(arrays.getArrays())
                .map(array -> {
                    String id = array.getType().getIdentifier();
                    long count = countByType.getOrDefault(id, 0L);
                    if (count <= 0) {
                        return null;
                    }
                    IType<IItem> itemType = typeById.get(id);
                    List<JfrMetadataField> fields = itemType == null ? List.of()
                            : itemType.getAttributes().stream()
                                    .map(this::toField)
                                    .sorted(Comparator.comparing(JfrMetadataField::label)
                                            .thenComparing(JfrMetadataField::id))
                                    .toList();
                    return new JfrMetadataEventType(id,
                            blankToDefault(array.getType().getName(), id),
                            categoryPath(array.getTypeCategory()),
                            count,
                            "",
                            fields);
                })
                .filter(Objects::nonNull)
                .toList();
        return new JfrMetadataReport(eventTypes);
    }

    private JfrMetadataField toField(IAttribute<?> attribute) {
        return new JfrMetadataField(attribute.getIdentifier(),
                blankToDefault(attribute.getName(), attribute.getIdentifier()),
                englishDescription(attribute),
                valueType(attribute),
                unit(attribute));
    }

    private EventValueType valueType(IAttribute<?> attribute) {
        String contentType = attribute.getKey().getContentType() == null
                ? "" : attribute.getKey().getContentType().getIdentifier().toLowerCase(Locale.ROOT);
        String identifier = attribute.getIdentifier() == null
                ? "" : attribute.getIdentifier().toLowerCase(Locale.ROOT);
        if (identifier.contains("time") || contentType.contains("timestamp")) {
            return EventValueType.TIMESTAMP;
        }
        if (contentType.contains("timespan") || identifier.contains("duration")) {
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

    private String unit(IAttribute<?> attribute) {
        return attribute.getKey().getContentType() == null ? "" : attribute.getKey().getContentType().getIdentifier();
    }

    private String englishDescription(IAttribute<?> attribute) {
        return attribute.getDescription() == null ? "" : attribute.getDescription();
    }

    private List<String> categoryPath(String[] category) {
        if (category == null || category.length == 0) {
            return List.of("Uncategorized");
        }
        List<String> path = Arrays.stream(category)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        return path.isEmpty() ? List.of("Uncategorized") : path;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
