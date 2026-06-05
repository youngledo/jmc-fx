package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventWindow;
import io.github.youngledo.jmcfx.domain.model.EventWindowRequest;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface EventQueryService {
    EventQuerySession openSession(RecordingSummary recording);

    default List<EventTypeNode> loadEventTypeTree(RecordingSummary recording) {
        try (EventQuerySession session = openSession(recording)) {
            return session.loadEventTypeTree();
        }
    }

    default List<EventFieldDescriptor> loadFieldDescriptors(RecordingSummary recording, String eventTypeId) {
        try (EventQuerySession session = openSession(recording)) {
            return session.loadFieldDescriptors(eventTypeId);
        }
    }

    default EventWindow loadEventWindow(RecordingSummary recording, EventWindowRequest request) {
        try (EventQuerySession session = openSession(recording)) {
            return session.loadEventWindow(request);
        }
    }

    default EventDetails loadEventDetails(RecordingSummary recording, String eventId) {
        try (EventQuerySession session = openSession(recording)) {
            return session.loadEventDetails(eventId);
        }
    }
}
