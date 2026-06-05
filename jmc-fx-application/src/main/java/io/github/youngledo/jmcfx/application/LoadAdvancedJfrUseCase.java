package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.EventHeatmap;
import io.github.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;

public final class LoadAdvancedJfrUseCase {

    private final AdvancedJfrAnalysisService service;

    public LoadAdvancedJfrUseCase(AdvancedJfrAnalysisService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes) {
        return service.loadEventHeatmap(recording, bucketCount, maxEventTypes);
    }

    public MemoryAnalysisReport loadMemoryAnalysis(RecordingSummary recording, int maxIssues) {
        return service.loadMemoryAnalysis(recording, maxIssues);
    }
}
