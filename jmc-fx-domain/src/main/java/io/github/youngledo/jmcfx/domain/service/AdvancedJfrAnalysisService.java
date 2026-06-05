package io.github.youngledo.jmcfx.domain.service;

import io.github.youngledo.jmcfx.domain.model.EventHeatmap;
import io.github.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface AdvancedJfrAnalysisService {

    EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes);

    MemoryAnalysisReport loadMemoryAnalysis(RecordingSummary recording, int maxIssues);
}
