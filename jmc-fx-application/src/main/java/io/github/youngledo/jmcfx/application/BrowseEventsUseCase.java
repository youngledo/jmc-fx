package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.EventQueryService;

public final class BrowseEventsUseCase {

    private final EventQueryService eventQueryService;

    public BrowseEventsUseCase(EventQueryService eventQueryService) {
        this.eventQueryService = Objects.requireNonNull(eventQueryService, "eventQueryService");
    }

    public EventBrowserSession openSession(RecordingSummary recording) {
        Objects.requireNonNull(recording, "recording");
        return new EventBrowserSession(eventQueryService.openSession(recording));
    }

    public static BrowseEventsUseCase unavailable() {
        return new BrowseEventsUseCase(recording -> {
            throw new UnsupportedOperationException("Event browser service is not connected for this workspace.");
        });
    }
}
