package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.HeapService;

public class FakeHeapService implements HeapService {

    private final List<HeapClassHistogram> histogram = new ArrayList<>();
    private ChartDefinition timeline = new ChartDefinition("Time", "Bytes", List.of());

    public void addHistogramRow(HeapClassHistogram row) {
        histogram.add(row);
    }

    public void setTimeline(ChartDefinition timeline) {
        this.timeline = timeline;
    }

    @Override
    public List<HeapClassHistogram> loadHeapClassHistogram(RecordingSummary recording) {
        return List.copyOf(histogram);
    }

    @Override
    public ChartDefinition loadHeapUsageTimeline(RecordingSummary recording) {
        return timeline;
    }
}
