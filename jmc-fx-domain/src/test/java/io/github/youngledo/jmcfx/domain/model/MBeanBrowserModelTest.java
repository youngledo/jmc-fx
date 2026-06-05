package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class MBeanBrowserModelTest {

    @Test
    void mbeanNodeNormalizesTextAndCopiesChildren() {
        MBeanNode child = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanNode node = MBeanNode.domain(null, List.of(child));

        assertEquals("", node.name());
        assertEquals("", node.objectName());
        assertEquals(1, node.children().size());
        assertThrows(UnsupportedOperationException.class,
                () -> node.children().add(MBeanNode.domain("x", List.of())));
    }

    @Test
    void attributeInfoNormalizesValues() {
        MBeanAttributeInfo attribute = new MBeanAttributeInfo(null, null, true, false, null, null);

        assertEquals("", attribute.name());
        assertEquals("", attribute.type());
        assertEquals("", attribute.value());
        assertEquals("", attribute.error());
    }

    @Test
    void operationInfoCopiesParameters() {
        MBeanOperationInfo operation = new MBeanOperationInfo("gc", "void", null,
                List.of(new MBeanOperationParameter("verbose", "boolean", null)));

        assertEquals("gc", operation.name());
        assertEquals("void", operation.returnType());
        assertEquals("", operation.description());
        assertEquals("boolean", operation.parameters().getFirst().type());
        assertThrows(UnsupportedOperationException.class,
                () -> operation.parameters().add(new MBeanOperationParameter("x", "java.lang.String", "")));
    }

    @Test
    void operationRequestRequiresConnection() {
        assertThrows(NullPointerException.class,
                () -> new MBeanOperationRequest(null, "java.lang:type=Memory", "gc",
                        List.of("boolean"), List.of("true")));

        MBeanOperationRequest request = new MBeanOperationRequest(
                new JvmConnection("local", "Local JVM", "service:jmx:rmi://local", true),
                "java.lang:type=Memory", "gc", null, List.of());

        assertEquals(List.of(), request.parameterTypes());
        assertThrows(UnsupportedOperationException.class,
                () -> request.parameterTypes().add("java.lang.String"));
    }

    @Test
    void operationResultDefaultsSuccessAndNormalizesText() {
        MBeanOperationResult result = new MBeanOperationResult(true, null, null);

        assertEquals("", result.value());
        assertEquals("", result.error());
    }

    @Test
    void jmxAttributeSubscriptionNormalizesBlankTextAndBoundsRetention() {
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "",
                "",
                "java.lang:type=Memory",
                "HeapMemoryUsage",
                "",
                "",
                Duration.ZERO,
                -1,
                true,
                true);

        assertFalse(subscription.id().isBlank());
        assertEquals("", subscription.connectionId());
        assertEquals("java.lang:type=Memory", subscription.objectName());
        assertEquals("HeapMemoryUsage", subscription.attributeName());
        assertEquals("HeapMemoryUsage", subscription.label());
        assertEquals("", subscription.unit());
        assertEquals(Duration.ofSeconds(1), subscription.samplingInterval());
        assertEquals(120, subscription.maxSamples());
    }

    @Test
    void jmxSubscriptionSampleKeepsNumericAndDisplayValues() {
        JmxSubscriptionSample sample = new JmxSubscriptionSample(
                "sub-1",
                Instant.parse("2026-05-29T00:00:00Z"),
                42.5,
                "42.5 MB",
                "MB",
                true);

        assertEquals("sub-1", sample.subscriptionId());
        assertEquals(Instant.parse("2026-05-29T00:00:00Z"), sample.observedAt());
        assertEquals(42.5, sample.numericValue());
        assertEquals("42.5 MB", sample.displayValue());
        assertEquals("MB", sample.unit());
        assertTrue(sample.numeric());
    }

    @Test
    void jmxNotificationEventNormalizesNulls() {
        JmxNotificationEvent event = new JmxNotificationEvent(
                null,
                null,
                null,
                null,
                7,
                null,
                null);

        assertEquals("", event.subscriptionId());
        assertEquals(Instant.EPOCH, event.observedAt());
        assertEquals("", event.type());
        assertEquals("", event.source());
        assertEquals(7, event.sequenceNumber());
        assertEquals("", event.message());
        assertEquals("", event.userData());
    }

    @Test
    void jfrMetadataFieldNormalizesNullText() {
        JfrMetadataField field = new JfrMetadataField(null, null, null, EventValueType.NUMBER, null);

        assertEquals("", field.id());
        assertEquals("", field.label());
        assertEquals("", field.description());
        assertEquals(EventValueType.NUMBER, field.valueType());
        assertEquals("", field.unit());
    }

    @Test
    void jfrMetadataEventTypeNormalizesSummaryAndFields() {
        JfrMetadataEventType eventType = new JfrMetadataEventType(
                "jdk.CPULoad",
                "CPU Load",
                List.of("Operating System"),
                7,
                null,
                List.of(new JfrMetadataField("jvmUser", "JVM User", "", EventValueType.NUMBER, "%")));

        assertEquals("jdk.CPULoad", eventType.id());
        assertEquals("CPU Load", eventType.name());
        assertEquals(List.of("Operating System"), eventType.categoryPath());
        assertEquals("Operating System", eventType.category());
        assertEquals(7, eventType.eventCount());
        assertEquals(1, eventType.fieldCount());
        assertEquals("", eventType.description());
    }

    @Test
    void jfrMetadataReportComputesTotals() {
        JfrMetadataReport report = new JfrMetadataReport(List.of(
                new JfrMetadataEventType("jdk.CPULoad", "CPU Load", List.of("Operating System"),
                        2, "", List.of(new JfrMetadataField("jvmUser", "JVM User", "", EventValueType.NUMBER, "%"))),
                new JfrMetadataEventType("jdk.ThreadStart", "Thread Start", List.of("Java Application"),
                        3, "", List.of(new JfrMetadataField("eventThread", "Thread", "", EventValueType.TEXT, "")))));

        assertEquals(2, report.eventTypeCount());
        assertEquals(5, report.eventCount());
        assertEquals(2, report.fieldCount());
    }
}
