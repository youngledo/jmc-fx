package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.domain.service.TlabService;

public final class LoadTlabUseCase {

    private final TlabService service;

    public LoadTlabUseCase(TlabService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<TlabAllocation> loadTlabAllocations(RecordingSummary recording) {
        return service.loadTlabAllocations(recording);
    }

    public ChartDefinition loadTlabAllocationTimeline(RecordingSummary recording) {
        return service.loadTlabAllocationTimeline(recording);
    }
}
