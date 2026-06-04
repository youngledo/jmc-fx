package com.youngledo.jmcfx.adapter.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import org.junit.jupiter.api.Test;

class JavaJmxMonitoringRepositoryTest {

    private static final String LEGACY_NODE_PATH = "/com/youngledo/jmcfx/ui/preferences";

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
    void storesSamplesAcrossBoundedPreferenceValues() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 120, true, true);
        repository.saveAttributeSubscription(subscription);

        for (int index = 0; index < 120; index++) {
            repository.appendSample(new JmxSubscriptionSample(
                    "sub-1",
                    Instant.EPOCH.plusSeconds(index),
                    index,
                    "sample-" + index + "-" + "x".repeat(150),
                    "%",
                    true));
        }

        assertEquals(120, repository.findSamples("sub-1").size());
        assertTrue(repository.rawValues().stream().allMatch(value -> value.length() <= Preferences.MAX_VALUE_LENGTH));
    }

    @Test
    void storesLargeSampleIndexesAcrossBoundedPreferenceValues() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 600, true, true);
        repository.saveAttributeSubscription(subscription);

        for (int index = 0; index < 600; index++) {
            repository.appendSample(new JmxSubscriptionSample(
                    "sub-1",
                    Instant.EPOCH.plusSeconds(index),
                    index,
                    "sample-" + index,
                    "%",
                    true));
        }

        assertEquals(600, repository.findSamples("sub-1").size());
        assertTrue(repository.rawValues().stream().allMatch(value -> value.length() <= Preferences.MAX_VALUE_LENGTH));
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
    void defaultRepositoryUsesLegacyUiPreferenceNodeAfterAdapterMove() throws BackingStoreException {
        Preferences legacyNode = legacyNode();
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "legacy-sub-" + UUID.randomUUID(), "legacy-connection", "java.lang:type=Memory",
                "HeapMemoryUsage", "Heap", "%", Duration.ofSeconds(1), 2, true, true);

        try {
            new JavaJmxMonitoringRepository().saveAttributeSubscription(subscription);

            assertTrue(legacyNode.get("attribute." + subscription.id(), "").contains(encoded("legacy-connection")));
        } finally {
            new JavaJmxMonitoringRepository().deleteAttributeSubscription(subscription.id());
            legacyNode.remove("attribute." + subscription.id());
        }
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
    void storesNotificationEventsAcrossBoundedPreferenceValues() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 200, true, true);
        repository.saveNotificationSubscription(subscription);

        for (int index = 0; index < 200; index++) {
            repository.appendNotificationEvent(new JmxNotificationEvent(
                    "notif-1",
                    Instant.EPOCH.plusSeconds(index),
                    "demo.type",
                    "demo.source",
                    index,
                    "message-" + index + "-" + "x".repeat(150),
                    "user-data"));
        }

        assertEquals(200, repository.findNotificationEvents("notif-1").size());
        assertTrue(repository.rawValues().stream().allMatch(value -> value.length() <= Preferences.MAX_VALUE_LENGTH));
    }

    @Test
    void storesLargeNotificationEventIndexesAcrossBoundedPreferenceValues() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 600, true, true);
        repository.saveNotificationSubscription(subscription);

        for (int index = 0; index < 600; index++) {
            repository.appendNotificationEvent(new JmxNotificationEvent(
                    "notif-1",
                    Instant.EPOCH.plusSeconds(index),
                    "demo.type",
                    "demo.source",
                    index,
                    "message-" + index,
                    "user-data"));
        }

        assertEquals(600, repository.findNotificationEvents("notif-1").size());
        assertTrue(repository.rawValues().stream().allMatch(value -> value.length() <= Preferences.MAX_VALUE_LENGTH));
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
        repository.putRaw("samples.sub-1.ids", encoded("bad-row") + "\nnot-base64\n" + encoded("row-1"));
        repository.putRaw("samples.sub-1.bad-row", "broken");
        repository.putRaw("samples.sub-1.row-1", sampleEntry(new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH, 1, "1", "%", true)));

        assertEquals(List.of(valid), repository.findAttributeSubscriptions("42"));
        assertEquals(List.of(1.0), repository.findSamples("sub-1").stream()
                .map(JmxSubscriptionSample::numericValue)
                .toList());
    }

    @Test
    void ignoresAttributeRowsWithMalformedBooleansWithoutHidingValidEntries() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxAttributeSubscription valid = new JmxAttributeSubscription(
                "sub-1", "42", "java.lang:type=Memory", "HeapMemoryUsage",
                "Heap", "%", Duration.ofSeconds(1), 2, true, true);
        JmxAttributeSubscription malformed = new JmxAttributeSubscription(
                "sub-2", "42", "java.lang:type=Threading", "ThreadCount",
                "Threads", "threads", Duration.ofSeconds(1), 2, true, true);
        repository.putRaw("attributeSubscriptionIds", encoded("sub-2") + "\n" + encoded("sub-1"));
        repository.putRaw("attribute.sub-2", attributeEntry(malformed, "maybe", Boolean.toString(malformed.persisted())));
        repository.putRaw("attribute.sub-1", attributeEntry(valid));

        assertEquals(List.of(valid), repository.findAttributeSubscriptions("42"));
    }

    @Test
    void ignoresSampleRowsWithMalformedNumericBooleanWithoutHidingValidEntries() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxSubscriptionSample valid = new JmxSubscriptionSample("sub-1", Instant.EPOCH, 1, "1", "%", true);
        JmxSubscriptionSample malformed = new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH.plusSeconds(1), 2, "2", "%", true);
        repository.putRaw("samples.sub-1.ids", encoded("bad-row") + "\n" + encoded("row-1"));
        repository.putRaw("samples.sub-1.bad-row", sampleEntry(malformed, "maybe"));
        repository.putRaw("samples.sub-1.row-1", sampleEntry(valid));

        assertEquals(List.of(valid), repository.findSamples("sub-1"));
    }

    @Test
    void ignoresMalformedChunkedSampleIndexEntriesWithoutHidingValidRows() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxSubscriptionSample first = new JmxSubscriptionSample("sub-1", Instant.EPOCH, 1, "1", "%", true);
        JmxSubscriptionSample second = new JmxSubscriptionSample(
                "sub-1", Instant.EPOCH.plusSeconds(1), 2, "2", "%", true);
        repository.putRaw("samples.sub-1.ids.chunkCount", "2");
        repository.putRaw("samples.sub-1.ids.chunk.0", "not-base64\n" + encoded("row-1"));
        repository.putRaw("samples.sub-1.ids.chunk.1", encoded("missing-row") + "\n" + encoded("row-2"));
        repository.putRaw("samples.sub-1.row-1", sampleEntry(first));
        repository.putRaw("samples.sub-1.row-2", sampleEntry(second));

        assertEquals(List.of(first, second), repository.findSamples("sub-1"));
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
        repository.putRaw("notificationEvents.notif-1.ids", encoded("bad-row") + "\nnot-base64\n" + encoded("row-1"));
        repository.putRaw("notificationEvents.notif-1.bad-row", "broken");
        repository.putRaw("notificationEvents.notif-1.row-1", notificationEventEntry(validEvent));

        assertEquals(List.of(valid), repository.findNotificationSubscriptions("42"));
        assertEquals(List.of(validEvent), repository.findNotificationEvents("notif-1"));
    }

    @Test
    void ignoresNotificationRowsWithMalformedBooleansWithoutHidingValidEntries() {
        JavaJmxMonitoringRepository repository = JavaJmxMonitoringRepository.inMemory();
        JmxNotificationSubscription valid = new JmxNotificationSubscription(
                "notif-1", "42", "java.lang:type=Memory", "Memory", 2, true, true);
        JmxNotificationSubscription malformed = new JmxNotificationSubscription(
                "notif-2", "42", "java.lang:type=Threading", "Threads", 2, true, true);
        repository.putRaw("notificationSubscriptionIds", encoded("notif-2") + "\n" + encoded("notif-1"));
        repository.putRaw("notification.notif-2",
                notificationEntry(malformed, Boolean.toString(malformed.enabled()), "maybe"));
        repository.putRaw("notification.notif-1", notificationEntry(valid));

        assertEquals(List.of(valid), repository.findNotificationSubscriptions("42"));
    }

    private static JmxNotificationEvent notificationEvent(String subscriptionId, long sequenceNumber) {
        return new JmxNotificationEvent(subscriptionId, Instant.EPOCH.plusSeconds(sequenceNumber),
                "demo.type", "demo.source", sequenceNumber, "message " + sequenceNumber, "user data");
    }

    private static String attributeEntry(JmxAttributeSubscription subscription) {
        return attributeEntry(subscription, Boolean.toString(subscription.enabled()),
                Boolean.toString(subscription.persisted()));
    }

    private static String attributeEntry(
            JmxAttributeSubscription subscription,
            String enabled,
            String persisted) {
        return String.join("|",
                encoded(subscription.id()),
                encoded(subscription.connectionId()),
                encoded(subscription.objectName()),
                encoded(subscription.attributeName()),
                encoded(subscription.label()),
                encoded(subscription.unit()),
                encoded(Long.toString(subscription.samplingInterval().toMillis())),
                encoded(Integer.toString(subscription.maxSamples())),
                encoded(enabled),
                encoded(persisted));
    }

    private static String sampleEntry(JmxSubscriptionSample sample) {
        return sampleEntry(sample, Boolean.toString(sample.numeric()));
    }

    private static String sampleEntry(JmxSubscriptionSample sample, String numeric) {
        return String.join("|",
                encoded(sample.subscriptionId()),
                encoded(sample.observedAt().toString()),
                encoded(Double.toString(sample.numericValue())),
                encoded(sample.displayValue()),
                encoded(sample.unit()),
                encoded(numeric));
    }

    private static String notificationEntry(JmxNotificationSubscription subscription) {
        return notificationEntry(subscription, Boolean.toString(subscription.enabled()),
                Boolean.toString(subscription.persisted()));
    }

    private static String notificationEntry(
            JmxNotificationSubscription subscription,
            String enabled,
            String persisted) {
        return String.join("|",
                encoded(subscription.id()),
                encoded(subscription.connectionId()),
                encoded(subscription.objectName()),
                encoded(subscription.label()),
                encoded(Integer.toString(subscription.maxEvents())),
                encoded(enabled),
                encoded(persisted));
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

    private static Preferences legacyNode() throws BackingStoreException {
        return Preferences.userRoot().node(LEGACY_NODE_PATH);
    }
}
