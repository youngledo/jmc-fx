package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.domain.service.TlabService;

public class FakeTlabService implements TlabService {

    private final List<TlabAllocation> allocations = new ArrayList<>();
    private ChartDefinition timeline = new ChartDefinition("Time", "Bytes", List.of());

    public void addAllocation(TlabAllocation allocation) {
        allocations.add(allocation);
    }

    public void setTimeline(ChartDefinition timeline) {
        this.timeline = timeline;
    }

    @Override
    public List<TlabAllocation> loadTlabAllocations(RecordingSummary recording) {
        return List.copyOf(allocations);
    }

    @Override
    public ChartDefinition loadTlabAllocationTimeline(RecordingSummary recording) {
        return timeline;
    }
}
