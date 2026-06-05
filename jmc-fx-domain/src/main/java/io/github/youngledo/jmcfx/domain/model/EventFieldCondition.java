package io.github.youngledo.jmcfx.domain.model;

/// One structured field filter condition.
public record EventFieldCondition(
        String fieldId,
        EventFilterOperator operator,
        String value) {

    public EventFieldCondition {
        if (fieldId == null || fieldId.isBlank()) {
            throw new IllegalArgumentException("fieldId must not be blank");
        }
        value = value == null ? "" : value;
    }
}
