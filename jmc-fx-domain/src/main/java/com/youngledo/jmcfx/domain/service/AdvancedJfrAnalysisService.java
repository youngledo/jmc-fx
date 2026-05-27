package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface AdvancedJfrAnalysisService {

    EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes);

    default MemoryAnalysisReport loadMemoryAnalysis(RecordingSummary recording, int maxIssues) {
        return new MemoryAnalysisReport(0, 0, List.of());
    }
}
