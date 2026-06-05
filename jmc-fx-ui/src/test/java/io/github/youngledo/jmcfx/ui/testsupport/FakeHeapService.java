package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.HeapService;

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
