package com.youngledo.jmcfx.domain.model;

import java.util.Comparator;
import java.util.List;

public record JfrMetadataReport(List<JfrMetadataEventType> eventTypes) {

    public JfrMetadataReport {
        eventTypes = eventTypes == null ? List.of() : eventTypes.stream()
                .sorted(Comparator.comparing(JfrMetadataEventType::category)
                        .thenComparing(JfrMetadataEventType::name)
                        .thenComparing(JfrMetadataEventType::id))
                .toList();
    }

    public int eventTypeCount() {
        return eventTypes.size();
    }

    public long eventCount() {
        return eventTypes.stream().mapToLong(JfrMetadataEventType::eventCount).sum();
    }

    public int fieldCount() {
        return eventTypes.stream().mapToInt(JfrMetadataEventType::fieldCount).sum();
    }
}
