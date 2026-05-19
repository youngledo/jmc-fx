package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record EventDetails(
        String eventId,
        String eventTypeId,
        List<EventProperty> properties,
        EventTiming timing,
        EventThreadInfo thread,
        List<EventStackFrame> stackTrace) {

    public EventDetails {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (eventTypeId == null || eventTypeId.isBlank()) {
            throw new IllegalArgumentException("eventTypeId must not be blank");
        }
        properties = List.copyOf(properties);
        stackTrace = List.copyOf(stackTrace);
    }
}
