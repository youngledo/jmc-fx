package io.github.youngledo.jmcfx.domain.model;

/// UI-neutral table column definition for event rows.
public record EventColumn(
        String id,
        String label,
        String fieldId,
        EventColumnKind kind,
        int width,
        boolean removable) {

    public EventColumn {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        fieldId = fieldId == null ? "" : fieldId;
    }

    public static EventColumn common(String id, String label, int width) {
        return new EventColumn(id, label, "", EventColumnKind.COMMON, width, false);
    }

    public static EventColumn field(EventFieldDescriptor descriptor, int width) {
        return new EventColumn("field:" + descriptor.id(), descriptor.label(), descriptor.id(), EventColumnKind.FIELD,
                width, true);
    }
}
