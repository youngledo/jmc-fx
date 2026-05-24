package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.ChartSeriesType;

class JmcTlabServiceTest {

    @Test
    void timelineUsesAggregatedLineSeriesInsteadOfOneBarPerAllocation() {
        assertEquals(ChartSeriesType.LINE, JmcTlabService.timelineSeriesType(),
                "TLAB allocation recordings can contain huge event counts; the chart must be aggregated");
    }
}
