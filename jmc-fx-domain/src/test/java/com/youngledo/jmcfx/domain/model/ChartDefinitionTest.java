package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChartDefinitionTest {

    @Test
    void defaultsXAxisTypeToNumberForExistingCallers() {
        ChartDefinition definition = new ChartDefinition("Count", "Value", List.of());

        assertEquals(ChartXAxisType.NUMBER, definition.xAxisType());
    }

    @Test
    void carriesExplicitEpochSecondsXAxisType() {
        ChartDefinition definition = new ChartDefinition(
                "Time",
                "Count",
                ChartXAxisType.EPOCH_SECONDS,
                List.of());

        assertEquals("Time", definition.xLabel());
        assertEquals("Count", definition.yLabel());
        assertEquals(ChartXAxisType.EPOCH_SECONDS, definition.xAxisType());
    }
}
