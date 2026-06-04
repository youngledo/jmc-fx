package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.domain.service.TlabService;

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
