package com.youngledo.jmcfx.testsupport;

import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;

public class FakeAdvancedJfrAnalysisService implements AdvancedJfrAnalysisService {

    private EventHeatmap heatmap = new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1, List.of());
    private int lastBucketCount;
    private int lastMaxEventTypes;

    public void setHeatmap(EventHeatmap heatmap) {
        this.heatmap = heatmap;
    }

    @Override
    public EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes) {
        lastBucketCount = bucketCount;
        lastMaxEventTypes = maxEventTypes;
        return heatmap;
    }

    public int lastBucketCount() {
        return lastBucketCount;
    }

    public int lastMaxEventTypes() {
        return lastMaxEventTypes;
    }
}
