package com.youngledo.jmcfx.ui.preferences;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;

import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.service.JmxMonitoringRepository;

public final class JavaJmxMonitoringRepository implements JmxMonitoringRepository {

    static final String ATTRIBUTE_IDS = "attributeSubscriptionIds";
    static final String ATTRIBUTE_PREFIX = "attribute.";
    static final String SAMPLE_PREFIX = "samples.";
    static final String NOTIFICATION_IDS = "notificationSubscriptionIds";
    static final String NOTIFICATION_PREFIX = "notification.";
    static final String NOTIFICATION_EVENT_PREFIX = "notificationEvents.";
    static final String FIELD_SEPARATOR = "\\|";
    static final String ENTRY_SEPARATOR = "\n";

    private static final int DEFAULT_MAX_SAMPLES = 120;
    private static final int DEFAULT_MAX_NOTIFICATION_EVENTS = 200;

    private final Preferences preferences;

    public JavaJmxMonitoringRepository() {
        this(Preferences.userNodeForPackage(JavaJmxMonitoringRepository.class));
    }

    private JavaJmxMonitoringRepository(Preferences preferences) {
        this.preferences = preferences;
    }

    public static JavaJmxMonitoringRepository inMemory() {
        return new JavaJmxMonitoringRepository(new InMemoryPreferences());
    }

    @Override
    public List<JmxAttributeSubscription> findAttributeSubscriptions(String connectionId) {
        return readAttributeSubscriptions().stream()
                .filter(subscription -> subscription.connectionId().equals(connectionId))
                .toList();
    }

    @Override
    public void saveAttributeSubscription(JmxAttributeSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        List<String> ids = readIds(ATTRIBUTE_IDS);
        writeAttributeSubscription(subscription);
        if (!ids.contains(subscription.id())) {
            ids.add(subscription.id());
            writeIds(ATTRIBUTE_IDS, ids);
        }
    }

    @Override
    public void deleteAttributeSubscription(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }
        List<String> ids = readIds(ATTRIBUTE_IDS);
        ids.removeIf(subscriptionId::equals);
        writeIds(ATTRIBUTE_IDS, ids);
        preferences.remove(attributeKey(subscriptionId));
        preferences.remove(samplesKey(subscriptionId));
    }

    @Override
    public List<JmxSubscriptionSample> findSamples(String subscriptionId) {
        return readEntries(samplesKey(subscriptionId), JavaJmxMonitoringRepository::decodeSample);
    }

    @Override
    public void appendSample(JmxSubscriptionSample sample) {
        Objects.requireNonNull(sample, "sample");
        List<JmxSubscriptionSample> samples = new ArrayList<>(findSamples(sample.subscriptionId()));
        samples.add(sample);
        int maxSamples = readAttributeSubscription(sample.subscriptionId())
                .map(JmxAttributeSubscription::maxSamples)
                .orElse(DEFAULT_MAX_SAMPLES);
        writeSamples(sample.subscriptionId(), trimToNewest(samples, maxSamples));
    }

    @Override
    public List<JmxNotificationSubscription> findNotificationSubscriptions(String connectionId) {
        return readNotificationSubscriptions().stream()
                .filter(subscription -> subscription.connectionId().equals(connectionId))
                .toList();
    }

    @Override
    public void saveNotificationSubscription(JmxNotificationSubscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        List<String> ids = readIds(NOTIFICATION_IDS);
        writeNotificationSubscription(subscription);
        if (!ids.contains(subscription.id())) {
            ids.add(subscription.id());
            writeIds(NOTIFICATION_IDS, ids);
        }
    }

    @Override
    public void deleteNotificationSubscription(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }
        List<String> ids = readIds(NOTIFICATION_IDS);
        ids.removeIf(subscriptionId::equals);
        writeIds(NOTIFICATION_IDS, ids);
        preferences.remove(notificationKey(subscriptionId));
        preferences.remove(notificationEventsKey(subscriptionId));
    }

    @Override
    public List<JmxNotificationEvent> findNotificationEvents(String subscriptionId) {
        return readEntries(notificationEventsKey(subscriptionId), JavaJmxMonitoringRepository::decodeNotificationEvent);
    }

    @Override
    public void appendNotificationEvent(JmxNotificationEvent event) {
        Objects.requireNonNull(event, "event");
        List<JmxNotificationEvent> events = new ArrayList<>(findNotificationEvents(event.subscriptionId()));
        events.add(event);
        int maxEvents = readNotificationSubscription(event.subscriptionId())
                .map(JmxNotificationSubscription::maxEvents)
                .orElse(DEFAULT_MAX_NOTIFICATION_EVENTS);
        writeNotificationEvents(event.subscriptionId(), trimToNewest(events, maxEvents));
    }

    void putRaw(String key, String value) {
        preferences.put(key, value);
    }

    private List<JmxAttributeSubscription> readAttributeSubscriptions() {
        List<JmxAttributeSubscription> subscriptions = new ArrayList<>();
        for (String id : readIds(ATTRIBUTE_IDS)) {
            readAttributeSubscription(id).ifPresent(subscriptions::add);
        }
        return List.copyOf(subscriptions);
    }

    private Optional<JmxAttributeSubscription> readAttributeSubscription(String id) {
        String persisted = preferences.get(attributeKey(id), "");
        if (persisted.isBlank()) {
            return Optional.empty();
        }
        return decodeAttributeSubscription(persisted);
    }

    private void writeAttributeSubscription(JmxAttributeSubscription subscription) {
        preferences.put(attributeKey(subscription.id()), encode(subscription));
    }

    private List<JmxNotificationSubscription> readNotificationSubscriptions() {
        List<JmxNotificationSubscription> subscriptions = new ArrayList<>();
        for (String id : readIds(NOTIFICATION_IDS)) {
            readNotificationSubscription(id).ifPresent(subscriptions::add);
        }
        return List.copyOf(subscriptions);
    }

    private Optional<JmxNotificationSubscription> readNotificationSubscription(String id) {
        String persisted = preferences.get(notificationKey(id), "");
        if (persisted.isBlank()) {
            return Optional.empty();
        }
        return decodeNotificationSubscription(persisted);
    }

    private void writeNotificationSubscription(JmxNotificationSubscription subscription) {
        preferences.put(notificationKey(subscription.id()), encode(subscription));
    }

    private List<String> readIds(String key) {
        String persisted = preferences.get(key, "");
        if (persisted.isBlank()) {
            return new ArrayList<>();
        }
        List<String> ids = new ArrayList<>();
        for (String encodedId : persisted.split(ENTRY_SEPARATOR)) {
            try {
                String id = decodeField(encodedId);
                if (!id.isBlank() && !ids.contains(id)) {
                    ids.add(id);
                }
            } catch (IllegalArgumentException exception) {
                // Ignore malformed index entries so one bad id does not hide the whole repository.
            }
        }
        return ids;
    }

    private void writeIds(String key, List<String> ids) {
        preferences.put(key, ids.stream()
                .map(JavaJmxMonitoringRepository::encodeField)
                .reduce((first, second) -> first + ENTRY_SEPARATOR + second)
                .orElse(""));
    }

    private void writeSamples(String subscriptionId, List<JmxSubscriptionSample> samples) {
        writeEntries(samplesKey(subscriptionId), samples.stream()
                .map(JavaJmxMonitoringRepository::encode)
                .toList());
    }

    private void writeNotificationEvents(String subscriptionId, List<JmxNotificationEvent> events) {
        writeEntries(notificationEventsKey(subscriptionId), events.stream()
                .map(JavaJmxMonitoringRepository::encode)
                .toList());
    }

    private void writeEntries(String key, List<String> encodedEntries) {
        preferences.put(key, String.join(ENTRY_SEPARATOR, encodedEntries));
    }

    private static <T> List<T> readEntries(String key, Decoder<T> decoder, Preferences preferences) {
        String persisted = preferences.get(key, "");
        if (persisted.isBlank()) {
            return List.of();
        }
        List<T> entries = new ArrayList<>();
        for (String entry : persisted.split(ENTRY_SEPARATOR)) {
            decoder.decode(entry).ifPresent(entries::add);
        }
        return List.copyOf(entries);
    }

    private <T> List<T> readEntries(String key, Decoder<T> decoder) {
        return readEntries(key, decoder, preferences);
    }

    private static Optional<JmxAttributeSubscription> decodeAttributeSubscription(String entry) {
        String[] fields = entry.split(FIELD_SEPARATOR, -1);
        if (fields.length != 10) {
            return Optional.empty();
        }
        try {
            return Optional.of(new JmxAttributeSubscription(
                    decodeField(fields[0]),
                    decodeField(fields[1]),
                    decodeField(fields[2]),
                    decodeField(fields[3]),
                    decodeField(fields[4]),
                    decodeField(fields[5]),
                    Duration.ofMillis(Long.parseLong(decodeField(fields[6]))),
                    Integer.parseInt(decodeField(fields[7])),
                    Boolean.parseBoolean(decodeField(fields[8])),
                    Boolean.parseBoolean(decodeField(fields[9]))));
        } catch (IllegalArgumentException | DateTimeException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    private static String encode(JmxAttributeSubscription subscription) {
        return String.join("|",
                encodeField(subscription.id()),
                encodeField(subscription.connectionId()),
                encodeField(subscription.objectName()),
                encodeField(subscription.attributeName()),
                encodeField(subscription.label()),
                encodeField(subscription.unit()),
                encodeField(Long.toString(subscription.samplingInterval().toMillis())),
                encodeField(Integer.toString(subscription.maxSamples())),
                encodeField(Boolean.toString(subscription.enabled())),
                encodeField(Boolean.toString(subscription.persisted())));
    }

    private static Optional<JmxSubscriptionSample> decodeSample(String entry) {
        String[] fields = entry.split(FIELD_SEPARATOR, -1);
        if (fields.length != 6) {
            return Optional.empty();
        }
        try {
            return Optional.of(new JmxSubscriptionSample(
                    decodeField(fields[0]),
                    Instant.parse(decodeField(fields[1])),
                    Double.parseDouble(decodeField(fields[2])),
                    decodeField(fields[3]),
                    decodeField(fields[4]),
                    Boolean.parseBoolean(decodeField(fields[5]))));
        } catch (IllegalArgumentException | DateTimeException exception) {
            return Optional.empty();
        }
    }

    private static String encode(JmxSubscriptionSample sample) {
        return String.join("|",
                encodeField(sample.subscriptionId()),
                encodeField(sample.observedAt().toString()),
                encodeField(Double.toString(sample.numericValue())),
                encodeField(sample.displayValue()),
                encodeField(sample.unit()),
                encodeField(Boolean.toString(sample.numeric())));
    }

    private static Optional<JmxNotificationSubscription> decodeNotificationSubscription(String entry) {
        String[] fields = entry.split(FIELD_SEPARATOR, -1);
        if (fields.length != 7) {
            return Optional.empty();
        }
        try {
            return Optional.of(new JmxNotificationSubscription(
                    decodeField(fields[0]),
                    decodeField(fields[1]),
                    decodeField(fields[2]),
                    decodeField(fields[3]),
                    Integer.parseInt(decodeField(fields[4])),
                    Boolean.parseBoolean(decodeField(fields[5])),
                    Boolean.parseBoolean(decodeField(fields[6]))));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String encode(JmxNotificationSubscription subscription) {
        return String.join("|",
                encodeField(subscription.id()),
                encodeField(subscription.connectionId()),
                encodeField(subscription.objectName()),
                encodeField(subscription.label()),
                encodeField(Integer.toString(subscription.maxEvents())),
                encodeField(Boolean.toString(subscription.enabled())),
                encodeField(Boolean.toString(subscription.persisted())));
    }

    private static Optional<JmxNotificationEvent> decodeNotificationEvent(String entry) {
        String[] fields = entry.split(FIELD_SEPARATOR, -1);
        if (fields.length != 7) {
            return Optional.empty();
        }
        try {
            return Optional.of(new JmxNotificationEvent(
                    decodeField(fields[0]),
                    Instant.parse(decodeField(fields[1])),
                    decodeField(fields[2]),
                    decodeField(fields[3]),
                    Long.parseLong(decodeField(fields[4])),
                    decodeField(fields[5]),
                    decodeField(fields[6])));
        } catch (IllegalArgumentException | DateTimeException exception) {
            return Optional.empty();
        }
    }

    private static String encode(JmxNotificationEvent event) {
        return String.join("|",
                encodeField(event.subscriptionId()),
                encodeField(event.observedAt().toString()),
                encodeField(event.type()),
                encodeField(event.source()),
                encodeField(Long.toString(event.sequenceNumber())),
                encodeField(event.message()),
                encodeField(event.userData()));
    }

    private static <T> List<T> trimToNewest(List<T> rows, int maxSize) {
        int fromIndex = Math.max(0, rows.size() - maxSize);
        return List.copyOf(rows.subList(fromIndex, rows.size()));
    }

    private static String attributeKey(String id) {
        return ATTRIBUTE_PREFIX + id;
    }

    private static String samplesKey(String subscriptionId) {
        return SAMPLE_PREFIX + subscriptionId;
    }

    private static String notificationKey(String id) {
        return NOTIFICATION_PREFIX + id;
    }

    private static String notificationEventsKey(String subscriptionId) {
        return NOTIFICATION_EVENT_PREFIX + subscriptionId;
    }

    private static String encodeField(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface Decoder<T> {
        Optional<T> decode(String entry);
    }

    private static final class InMemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();

        private InMemoryPreferences() {
            super(null, "");
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return new InMemoryPreferences();
        }

        @Override
        protected void syncSpi() {
        }

        @Override
        protected void flushSpi() {
        }
    }
}
