package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TriggerModelTest {

    @Test
    void metricDefinitionNormalizesText() {
        LiveMetricDefinition definition = new LiveMetricDefinition(
                LiveMetricKind.HEAP_USED_PERCENT, null, null, 80.0);

        assertEquals("", definition.label());
        assertEquals("", definition.unit());
        assertEquals(80.0, definition.defaultThreshold());
    }

    @Test
    void triggerOperatorEvaluatesNumericValues() {
        assertTrue(TriggerOperator.GREATER_THAN.test(10.0, 9.0));
        assertFalse(TriggerOperator.GREATER_THAN.test(10.0, 10.0));
        assertTrue(TriggerOperator.GREATER_THAN_OR_EQUAL.test(10.0, 10.0));
        assertTrue(TriggerOperator.LESS_THAN.test(4.0, 5.0));
        assertTrue(TriggerOperator.LESS_THAN_OR_EQUAL.test(5.0, 5.0));
    }

    @Test
    void triggerRuleEvaluatesMatchingMetricOnlyWhenEnabled() {
        TriggerRule enabled = new TriggerRule("rule-1", "Heap high", true,
                LiveMetricKind.HEAP_USED_PERCENT, TriggerOperator.GREATER_THAN_OR_EQUAL, 75.0,
                TriggerAction.notifyOnly());
        TriggerRule disabled = new TriggerRule("rule-2", "Heap high", false,
                LiveMetricKind.HEAP_USED_PERCENT, TriggerOperator.GREATER_THAN_OR_EQUAL, 75.0,
                TriggerAction.notifyOnly());

        assertTrue(enabled.matches(new LiveMetricSnapshot(LiveMetricKind.HEAP_USED_PERCENT, 75.0,
                "%", Instant.EPOCH)));
        assertFalse(enabled.matches(new LiveMetricSnapshot(LiveMetricKind.THREAD_COUNT, 75.0,
                "threads", Instant.EPOCH)));
        assertFalse(disabled.matches(new LiveMetricSnapshot(LiveMetricKind.HEAP_USED_PERCENT, 90.0,
                "%", Instant.EPOCH)));
    }

    @Test
    void commandActionCopiesArguments() {
        TriggerAction action = TriggerAction.diagnosticCommand("threadPrint", List.of("-l"));

        assertEquals(TriggerActionType.DIAGNOSTIC_COMMAND, action.type());
        assertEquals("threadPrint", action.commandName());
        assertEquals(List.of("-l"), action.arguments());
        assertThrows(UnsupportedOperationException.class, () -> action.arguments().add("-e"));
    }

    @Test
    void triggerEventNormalizesText() {
        TriggerEvent event = new TriggerEvent("rule-1", null,
                LiveMetricKind.THREAD_COUNT, 12.0, "threads", Instant.EPOCH, null);

        assertEquals("", event.ruleName());
        assertEquals("", event.message());
    }
}
