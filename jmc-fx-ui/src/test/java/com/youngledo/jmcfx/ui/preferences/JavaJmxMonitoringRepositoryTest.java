package com.youngledo.jmcfx.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import org.junit.jupiter.api.Test;

class JavaJmxMonitoringRepositoryTest {

    @Test
    void savesAttributeSubscriptionsPerConnection() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(2), 5, true, true);

        repository.saveAttributeSubscription(subscription);

        assertEquals(List.of(subscription), repository.findAttributeSubscriptions("42"));
        assertTrue(repository.findAttributeSubscriptions("99").isEmpty());
    }

    @Test
    void appendsSamplesWithBoundedRetentionFromSubscription() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 2, true, true);
        repository.saveAttributeSubscription(subscription);

        repository.appendSample(new JmxSubscriptionSample("sub-1", Instant.parse("2026-05-29T00:00:00Z"), 1, "1", "%", true));
        repository.appendSample(new JmxSubscriptionSample("sub-1", Instant.parse("2026-05-29T00:00:01Z"), 2, "2", "%", true));
        repository.appendSample(new JmxSubscriptionSample("sub-1", Instant.parse("2026-05-29T00:00:02Z"), 3, "3", "%", true));

        assertEquals(List.of(2.0, 3.0), repository.findSamples("sub-1").stream()
                .map(JmxSubscriptionSample::numericValue)
                .toList());
    }

    @Test
    void deletesSubscriptionAndItsSamples() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 2, true, true);
        repository.saveAttributeSubscription(subscription);
        repository.appendSample(new JmxSubscriptionSample("sub-1", Instant.EPOCH, 1, "1", "%", true));

        repository.deleteAttributeSubscription("sub-1");

        assertTrue(repository.findAttributeSubscriptions("42").isEmpty());
        assertTrue(repository.findSamples("sub-1").isEmpty());
    }

    @Test
    void savesNotificationSubscriptionsPerConnection() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 5, true, true);

        repository.saveNotificationSubscription(subscription);

        assertEquals(List.of(subscription), repository.findNotificationSubscriptions("42"));
        assertTrue(repository.findNotificationSubscriptions("99").isEmpty());
    }

    @Test
    void appendsNotificationEventsWithBoundedRetentionFromSubscription() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 2, true, true);
        repository.saveNotificationSubscription(subscription);

        repository.appendNotificationEvent(notificationEvent("notif-1", 1));
        repository.appendNotificationEvent(notificationEvent("notif-1", 2));
        repository.appendNotificationEvent(notificationEvent("notif-1", 3));

        assertEquals(List.of(2L, 3L), repository.findNotificationEvents("notif-1").stream()
                .map(JmxNotificationEvent::sequenceNumber)
                .toList());
    }

    @Test
    void deletesNotificationSubscriptionAndItsEvents() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 2, true, true);
        repository.saveNotificationSubscription(subscription);
        repository.appendNotificationEvent(notificationEvent("notif-1", 1));

        repository.deleteNotificationSubscription("notif-1");

        assertTrue(repository.findNotificationSubscriptions("42").isEmpty());
        assertTrue(repository.findNotificationEvents("notif-1").isEmpty());
    }

    @Test
    void returnsImmutableCopies() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription attributeSubscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 2, true, true);
        JmxNotificationSubscription notificationSubscription = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 2, true, true);
        repository.saveAttributeSubscription(attributeSubscription);
        repository.appendSample(new JmxSubscriptionSample("sub-1", Instant.EPOCH, 1, "1", "%", true));
        repository.saveNotificationSubscription(notificationSubscription);
        repository.appendNotificationEvent(notificationEvent("notif-1", 1));

        assertThrows(UnsupportedOperationException.class,
                () -> repository.findAttributeSubscriptions("42").add(attributeSubscription));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findSamples("sub-1").add(new JmxSubscriptionSample("sub-1", Instant.EPOCH, 2, "2", "%", true)));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findNotificationSubscriptions("42").add(notificationSubscription));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findNotificationEvents("notif-1").add(notificationEvent("notif-1", 2)));
    }

    @Test
    void ignoresMalformedAttributeRowsWithoutHidingValidEntries() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription valid = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 2, true, true);
        repository.putRaw("attributeSubscriptionIds", encoded("bad-index") + "\nnot-base64\n" + encoded("sub-1"));
        repository.putRaw("attribute.bad-index", "not-base64|fields");
        repository.putRaw("attribute.sub-1", attributeEntry(valid));
        repository.putRaw("samples.sub-1", "broken\n" + sampleEntry(new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH, 1, "1", "%", true)));

        assertEquals(List.of(valid), repository.findAttributeSubscriptions("42"));
        assertEquals(List.of(1.0), repository.findSamples("sub-1").stream()
                .map(JmxSubscriptionSample::numericValue)
                .toList());
    }

    @Test
    void ignoresMalformedNotificationRowsWithoutHidingValidEntries() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription valid = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 2, true, true);
        JmxNotificationEvent validEvent = notificationEvent("notif-1", 1);
        repository.putRaw("notificationSubscriptionIds", encoded("bad-index") + "\nnot-base64\n" + encoded("notif-1"));
        repository.putRaw("notification.bad-index", "not-base64|fields");
        repository.putRaw("notification.notif-1", notificationEntry(valid));
        repository.putRaw("notificationEvents.notif-1", "broken\n" + notificationEventEntry(validEvent));

        assertEquals(List.of(valid), repository.findNotificationSubscriptions("42"));
        assertEquals(List.of(validEvent), repository.findNotificationEvents("notif-1"));
    }

    private static JmxNotificationEvent notificationEvent(String subscriptionId, long sequenceNumber) {
        return new JmxNotificationEvent(subscriptionId, Instant.EPOCH.plusSeconds(sequenceNumber),
                "demo.type", "demo.source", sequenceNumber, "message " + sequenceNumber, "user data");
    }

    private static String attributeEntry(JmxAttributeSubscription subscription) {
        return String.join("|",
                encoded(subscription.id()),
                encoded(subscription.connectionId()),
                encoded(subscription.objectName()),
                encoded(subscription.attributeName()),
                encoded(subscription.label()),
                encoded(subscription.unit()),
                encoded(Long.toString(subscription.samplingInterval().toMillis())),
                encoded(Integer.toString(subscription.maxSamples())),
                encoded(Boolean.toString(subscription.enabled())),
                encoded(Boolean.toString(subscription.persisted())));
    }

    private static String sampleEntry(JmxSubscriptionSample sample) {
        return String.join("|",
                encoded(sample.subscriptionId()),
                encoded(sample.observedAt().toString()),
                encoded(Double.toString(sample.numericValue())),
                encoded(sample.displayValue()),
                encoded(sample.unit()),
                encoded(Boolean.toString(sample.numeric())));
    }

    private static String notificationEntry(JmxNotificationSubscription subscription) {
        return String.join("|",
                encoded(subscription.id()),
                encoded(subscription.connectionId()),
                encoded(subscription.objectName()),
                encoded(subscription.label()),
                encoded(Integer.toString(subscription.maxEvents())),
                encoded(Boolean.toString(subscription.enabled())),
                encoded(Boolean.toString(subscription.persisted())));
    }

    private static String notificationEventEntry(JmxNotificationEvent event) {
        return String.join("|",
                encoded(event.subscriptionId()),
                encoded(event.observedAt().toString()),
                encoded(event.type()),
                encoded(event.source()),
                encoded(Long.toString(event.sequenceNumber())),
                encoded(event.message()),
                encoded(event.userData()));
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
