package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;

public interface TlabService {
    List<TlabAllocation> loadTlabAllocations(RecordingSummary recording);
    ChartDefinition loadTlabAllocationTimeline(RecordingSummary recording);
}
