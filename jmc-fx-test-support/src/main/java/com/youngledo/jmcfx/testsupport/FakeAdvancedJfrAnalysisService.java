package com.youngledo.jmcfx.testsupport;

import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;

public class FakeAdvancedJfrAnalysisService implements AdvancedJfrAnalysisService {

    private EventHeatmap heatmap = new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1, List.of());
    private MemoryAnalysisReport memoryAnalysisReport = new MemoryAnalysisReport(0, 0, List.of());
    private int lastBucketCount;
    private int lastMaxEventTypes;
    private int lastMaxMemoryIssues;

    public void setHeatmap(EventHeatmap heatmap) {
        this.heatmap = heatmap;
    }

    public void setMemoryAnalysisReport(MemoryAnalysisReport memoryAnalysisReport) {
        this.memoryAnalysisReport = memoryAnalysisReport;
    }

    @Override
    public EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes) {
        lastBucketCount = bucketCount;
        lastMaxEventTypes = maxEventTypes;
        return heatmap;
    }

    @Override
    public MemoryAnalysisReport loadMemoryAnalysis(RecordingSummary recording, int maxIssues) {
        lastMaxMemoryIssues = maxIssues;
        return memoryAnalysisReport;
    }

    public int lastBucketCount() {
        return lastBucketCount;
    }

    public int lastMaxEventTypes() {
        return lastMaxEventTypes;
    }

    public int lastMaxMemoryIssues() {
        return lastMaxMemoryIssues;
    }
}
