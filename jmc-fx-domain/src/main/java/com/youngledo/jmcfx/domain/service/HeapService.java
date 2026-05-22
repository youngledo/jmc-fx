package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface HeapService {
    List<HeapClassHistogram> loadHeapClassHistogram(RecordingSummary recording);
    ChartDefinition loadHeapUsageTimeline(RecordingSummary recording);
}
