package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.HeapService;

public final class LoadHeapUseCase {

    private final HeapService service;

    public LoadHeapUseCase(HeapService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<HeapClassHistogram> loadHeapClassHistogram(RecordingSummary recording) {
        return service.loadHeapClassHistogram(recording);
    }

    public ChartDefinition loadHeapUsageTimeline(RecordingSummary recording) {
        return service.loadHeapUsageTimeline(recording);
    }
}
