package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RuleResultTest {

    @Test
    void createsRuleResultWithSeverityAndSummary() {
        RuleResult result = new RuleResult(
                "heap-pressure",
                "Heap Pressure",
                Severity.WARNING,
                65,
                "Memory",
                "Heap pressure detected",
                "Review allocation rate and GC pressure.");

        assertEquals("heap-pressure", result.id());
        assertEquals(Severity.WARNING, result.severity());
        assertEquals(65, result.score());
        assertEquals("Heap pressure detected", result.summary());
    }
}
