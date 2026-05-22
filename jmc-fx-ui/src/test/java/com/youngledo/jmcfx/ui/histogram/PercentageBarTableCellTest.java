package com.youngledo.jmcfx.ui.histogram;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Tests the percentage parsing logic used by PercentageBarTableCell.
/// Tests the pure-logic PercentageParser to avoid JavaFX toolkit initialization.
class PercentageBarTableCellTest {
    @Test
    void parsePercentage_validNumber() {
        assertEquals(0.75, PercentageParser.parsePercentage("75.0"), 0.001);
        assertEquals(0.50, PercentageParser.parsePercentage("50"), 0.001);
    }

    @Test
    void parsePercentage_clampsToRange() {
        assertEquals(1.0, PercentageParser.parsePercentage("150.0"), 0.001);
        assertEquals(0.0, PercentageParser.parsePercentage("-5.0"), 0.001);
    }

    @Test
    void parsePercentage_invalidReturnsZero() {
        assertEquals(0.0, PercentageParser.parsePercentage("abc"), 0.001);
        assertEquals(0.0, PercentageParser.parsePercentage(""), 0.001);
        assertEquals(0.0, PercentageParser.parsePercentage(null), 0.001);
    }
}
