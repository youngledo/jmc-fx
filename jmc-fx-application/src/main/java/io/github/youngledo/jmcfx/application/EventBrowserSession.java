package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventSelectionProperties;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventTypeSelection;
import io.github.youngledo.jmcfx.domain.model.EventWindow;
import io.github.youngledo.jmcfx.domain.model.EventWindowRequest;
import io.github.youngledo.jmcfx.domain.service.EventQuerySession;

public final class EventBrowserSession implements AutoCloseable {

    private final EventQuerySession delegate;

    EventBrowserSession(EventQuerySession delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public List<EventTypeNode> loadEventTypeTree() {
        return delegate.loadEventTypeTree();
    }

    public List<EventFieldDescriptor> loadFieldDescriptors(EventTypeSelection selection) {
        Objects.requireNonNull(selection, "selection");
        return delegate.loadFieldDescriptors(selection);
    }

    public EventWindow loadEventWindow(EventWindowRequest request) {
        Objects.requireNonNull(request, "request");
        return delegate.loadEventWindow(request);
    }

    public EventSelectionProperties loadSelectionProperties(EventTypeSelection selection) {
        Objects.requireNonNull(selection, "selection");
        return delegate.loadSelectionProperties(selection);
    }

    public EventDetails loadEventDetails(String eventId) {
        Objects.requireNonNull(eventId, "eventId");
        return delegate.loadEventDetails(eventId);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
