package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.LiveMetricService;

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
    void metricDefinitionRequiresKind() {
        assertThrows(NullPointerException.class,
                () -> new LiveMetricDefinition(null, "Heap used", "%", 80.0));
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
    void triggerOperatorExposesSymbols() {
        assertEquals(">", TriggerOperator.GREATER_THAN.symbol());
        assertEquals(">=", TriggerOperator.GREATER_THAN_OR_EQUAL.symbol());
        assertEquals("<", TriggerOperator.LESS_THAN.symbol());
        assertEquals("<=", TriggerOperator.LESS_THAN_OR_EQUAL.symbol());
    }

    @Test
    void liveMetricSnapshotNormalizesUnitAndObservedAt() {
        LiveMetricSnapshot snapshot = new LiveMetricSnapshot(
                LiveMetricKind.HEAP_USED_PERCENT, 42.0, null, null);

        assertEquals("", snapshot.unit());
        assertEquals(Instant.EPOCH, snapshot.observedAt());
    }

    @Test
    void liveMetricSnapshotRequiresKind() {
        assertThrows(NullPointerException.class,
                () -> new LiveMetricSnapshot(null, 42.0, "%", Instant.EPOCH));
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
    void triggerRuleNormalizesTextAndDoesNotMatchNullSnapshot() {
        TriggerRule rule = new TriggerRule(null, null, true,
                LiveMetricKind.HEAP_USED_PERCENT, TriggerOperator.GREATER_THAN_OR_EQUAL, 75.0,
                TriggerAction.notifyOnly());

        assertEquals("", rule.id());
        assertEquals("", rule.name());
        assertFalse(rule.matches(null));
    }

    @Test
    void triggerRuleRequiresMetricOperatorAndAction() {
        assertThrows(NullPointerException.class,
                () -> new TriggerRule("rule-1", "Heap high", true,
                        null, TriggerOperator.GREATER_THAN_OR_EQUAL, 75.0, TriggerAction.notifyOnly()));
        assertThrows(NullPointerException.class,
                () -> new TriggerRule("rule-1", "Heap high", true,
                        LiveMetricKind.HEAP_USED_PERCENT, null, 75.0, TriggerAction.notifyOnly()));
        assertThrows(NullPointerException.class,
                () -> new TriggerRule("rule-1", "Heap high", true,
                        LiveMetricKind.HEAP_USED_PERCENT, TriggerOperator.GREATER_THAN_OR_EQUAL, 75.0, null));
    }

    @Test
    void notifyOnlyActionUsesNotifyTypeAndEmptyImmutableArguments() {
        TriggerAction action = TriggerAction.notifyOnly();

        assertEquals(TriggerActionType.NOTIFY, action.type());
        assertEquals("", action.commandName());
        assertEquals(List.of(), action.arguments());
        assertThrows(UnsupportedOperationException.class, () -> action.arguments().add("-l"));
    }

    @Test
    void triggerActionRequiresType() {
        assertThrows(NullPointerException.class, () -> new TriggerAction(null, "", List.of()));
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
    void triggerActionNormalizesNullArguments() {
        TriggerAction action = TriggerAction.diagnosticCommand(null, null);

        assertEquals(TriggerActionType.DIAGNOSTIC_COMMAND, action.type());
        assertEquals("", action.commandName());
        assertEquals(List.of(), action.arguments());
    }

    @Test
    void triggerEventNormalizesText() {
        TriggerEvent event = new TriggerEvent("rule-1", null,
                LiveMetricKind.THREAD_COUNT, 12.0, "threads", Instant.EPOCH, null);

        assertEquals("", event.ruleName());
        assertEquals("", event.message());
    }

    @Test
    void triggerEventNormalizesRuleIdUnitAndFiredAt() {
        TriggerEvent event = new TriggerEvent(null, "Thread count high",
                LiveMetricKind.THREAD_COUNT, 12.0, null, null, "message");

        assertEquals("", event.ruleId());
        assertEquals("", event.unit());
        assertEquals(Instant.EPOCH, event.firedAt());
    }

    @Test
    void triggerEventRequiresMetric() {
        assertThrows(NullPointerException.class,
                () -> new TriggerEvent("rule-1", "Thread count high",
                        null, 12.0, "threads", Instant.EPOCH, "message"));
    }

    @Test
    void liveMetricServiceDefaultsRejectDefinitionsAndSnapshot() {
        LiveMetricService service = new LiveMetricService() {
        };
        JvmConnection connection = new JvmConnection("local", "Local JVM", "service:jmx:rmi:///jndi/rmi://localhost/jmxrmi",
                true);

        JmcFxException definitionsFailure = assertThrows(JmcFxException.class,
                () -> service.definitions(connection));
        JmcFxException snapshotFailure = assertThrows(JmcFxException.class,
                () -> service.snapshot(connection));

        assertEquals("Live JVM metrics are not supported by this service.", definitionsFailure.getMessage());
        assertEquals("Live JVM metrics are not supported by this service.", snapshotFailure.getMessage());
    }
}
