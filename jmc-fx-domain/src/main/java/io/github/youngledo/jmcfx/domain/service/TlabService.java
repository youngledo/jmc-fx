package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;

public interface TlabService {
    List<TlabAllocation> loadTlabAllocations(RecordingSummary recording);
    ChartDefinition loadTlabAllocationTimeline(RecordingSummary recording);
}
