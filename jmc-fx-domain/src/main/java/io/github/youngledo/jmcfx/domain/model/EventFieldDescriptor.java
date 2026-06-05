package io.github.youngledo.jmcfx.domain.model;

/// Metadata for one event field exposed through the adapter boundary.
public record EventFieldDescriptor(
        String id,
        String label,
        String description,
        EventValueType valueType,
        String unit,
        boolean recommendedColumn,
        boolean filterable,
        boolean sortable) {

    public EventFieldDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        description = description == null ? "" : description;
        unit = unit == null ? "" : unit;
    }
}
