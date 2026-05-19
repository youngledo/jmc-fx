package com.youngledo.jmcfx.domain.model;

import java.util.List;

/// UI-neutral selected Event Types node represented as concrete event type ids.
public record EventTypeSelection(String id, String label, List<String> eventTypeIds) {

    public static final String ALL_ID = "jmcfx.selection.all";

    public EventTypeSelection {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        eventTypeIds = List.copyOf(eventTypeIds);
        if (eventTypeIds.isEmpty()) {
            throw new IllegalArgumentException("eventTypeIds must not be empty");
        }
        if (eventTypeIds.stream().anyMatch(eventTypeId -> eventTypeId == null || eventTypeId.isBlank())) {
            throw new IllegalArgumentException("eventTypeIds must not contain blank values");
        }
    }

    public static EventTypeSelection single(String eventTypeId, String label) {
        return new EventTypeSelection(eventTypeId, label, List.of(eventTypeId));
    }

    public static EventTypeSelection group(String id, String label, List<String> eventTypeIds) {
        return new EventTypeSelection(id, label, eventTypeIds);
    }

    public static EventTypeSelection all(String label, List<String> eventTypeIds) {
        return new EventTypeSelection(ALL_ID, label, eventTypeIds);
    }

    public boolean singleType() {
        return eventTypeIds.size() == 1;
    }

    public String singleEventTypeIdOrBlank() {
        return singleType() ? eventTypeIds.getFirst() : "";
    }
}
