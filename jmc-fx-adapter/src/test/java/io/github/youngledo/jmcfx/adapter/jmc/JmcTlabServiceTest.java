package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.ChartSeriesType;
import io.github.youngledo.jmcfx.domain.model.ChartXAxisType;

class JmcTlabServiceTest {

    @Test
    void timelineUsesAggregatedLineSeriesInsteadOfOneBarPerAllocation() {
        assertEquals(ChartSeriesType.LINE, JmcTlabService.timelineSeriesType(),
                "TLAB allocation recordings can contain huge event counts; the chart must be aggregated");
    }

    @Test
    void timelineXAxisUsesEpochMillis() {
        assertEquals(ChartXAxisType.EPOCH_MILLIS, JmcTlabService.timelineXAxisType(),
                "TLAB allocation timeline buckets are epoch milliseconds and must format as timestamps");
    }
}
