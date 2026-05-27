package com.youngledo.jmcfx.domain.service;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface AdvancedJfrAnalysisService {

    EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes);
}
