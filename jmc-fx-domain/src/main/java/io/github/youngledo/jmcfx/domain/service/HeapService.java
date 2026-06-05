package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface HeapService {
    List<HeapClassHistogram> loadHeapClassHistogram(RecordingSummary recording);
    ChartDefinition loadHeapUsageTimeline(RecordingSummary recording);
}
