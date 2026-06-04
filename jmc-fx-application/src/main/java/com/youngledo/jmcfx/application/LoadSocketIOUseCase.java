package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;
import com.youngledo.jmcfx.domain.service.SocketIOService;

public final class LoadSocketIOUseCase {

    private final SocketIOService service;

    public LoadSocketIOUseCase(SocketIOService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<SocketIOHistogram> loadSocketIOHistogram(RecordingSummary recording, SocketIOGrouping grouping) {
        return service.loadSocketIOHistogram(recording, grouping);
    }

    public List<SocketIOEvent> loadSocketIOEvents(RecordingSummary recording) {
        return service.loadSocketIOEvents(recording);
    }

    public ChartDefinition loadTimeline(RecordingSummary recording) {
        return service.loadTimeline(recording);
    }
}
