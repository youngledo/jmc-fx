package io.github.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record JfrMetadataEventType(
        String id,
        String name,
        List<String> categoryPath,
        long eventCount,
        String description,
        List<JfrMetadataField> fields) {

    public JfrMetadataEventType {
        id = Objects.requireNonNullElse(id, "");
        name = Objects.requireNonNullElse(name, "");
        categoryPath = categoryPath == null ? List.of("Uncategorized") : List.copyOf(categoryPath);
        if (categoryPath.isEmpty()) {
            categoryPath = List.of("Uncategorized");
        }
        eventCount = Math.max(0, eventCount);
        description = Objects.requireNonNullElse(description, "");
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public String category() {
        return String.join(" / ", categoryPath);
    }

    public int fieldCount() {
        return fields.size();
    }
}
