package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.EventDetails;
import com.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import com.youngledo.jmcfx.domain.model.EventSelectionProperties;
import com.youngledo.jmcfx.domain.model.EventTypeNode;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.EventWindow;
import com.youngledo.jmcfx.domain.model.EventWindowRequest;

/// Closeable query session for one recording's Event Browser data.
public interface EventQuerySession extends AutoCloseable {
    List<EventTypeNode> loadEventTypeTree();

    default List<EventFieldDescriptor> loadFieldDescriptors(String eventTypeId) {
        return loadFieldDescriptors(EventTypeSelection.single(eventTypeId, eventTypeId));
    }

    List<EventFieldDescriptor> loadFieldDescriptors(EventTypeSelection selection);

    EventWindow loadEventWindow(EventWindowRequest request);

    EventSelectionProperties loadSelectionProperties(EventTypeSelection selection);

    EventDetails loadEventDetails(String eventId);

    @Override
    void close();
}
