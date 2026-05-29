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
import java.util.UUID;
import java.util.prefs.BackingStoreException;
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

    private static final String ENTRY_CHUNK_COUNT_SUFFIX = ".chunkCount";
    private static final String ENTRY_CHUNK_SUFFIX = ".chunk.";
    private static final int MAX_INDEX_CHUNK_LENGTH = Preferences.MAX_VALUE_LENGTH - 256;
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
        removeRows(sampleIndexPrefix(subscriptionId), rowId -> sampleRowKey(subscriptionId, rowId));
    }

    @Override
    public List<JmxSubscriptionSample> findSamples(String subscriptionId) {
        return readRows(sampleIndexPrefix(subscriptionId), rowId -> sampleRowKey(subscriptionId, rowId),
                JavaJmxMonitoringRepository::decodeSample).stream()
                .map(StoredRow::value)
                .toList();
    }

    @Override
    public void appendSample(JmxSubscriptionSample sample) {
        Objects.requireNonNull(sample, "sample");
        String indexKey = sampleIndexPrefix(sample.subscriptionId());
        RowKeyFactory rowKeyFactory = rowId -> sampleRowKey(sample.subscriptionId(), rowId);
        List<StoredRow<JmxSubscriptionSample>> samples = new ArrayList<>(
                readRows(indexKey, rowKeyFactory, JavaJmxMonitoringRepository::decodeSample));
        samples.add(new StoredRow<>(newRowId(samples.stream().map(StoredRow::id).toList()), sample));
        int maxSamples = readAttributeSubscription(sample.subscriptionId())
                .map(JmxAttributeSubscription::maxSamples)
                .orElse(DEFAULT_MAX_SAMPLES);
        writeRows(indexKey, rowKeyFactory, trimToNewest(samples, maxSamples), JavaJmxMonitoringRepository::encode);
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
        removeRows(notificationEventIndexPrefix(subscriptionId), rowId -> notificationEventRowKey(subscriptionId, rowId));
    }

    @Override
    public List<JmxNotificationEvent> findNotificationEvents(String subscriptionId) {
        return readRows(notificationEventIndexPrefix(subscriptionId), rowId -> notificationEventRowKey(subscriptionId, rowId),
                JavaJmxMonitoringRepository::decodeNotificationEvent).stream()
                .map(StoredRow::value)
                .toList();
    }

    @Override
    public void appendNotificationEvent(JmxNotificationEvent event) {
        Objects.requireNonNull(event, "event");
        String indexKey = notificationEventIndexPrefix(event.subscriptionId());
        RowKeyFactory rowKeyFactory = rowId -> notificationEventRowKey(event.subscriptionId(), rowId);
        List<StoredRow<JmxNotificationEvent>> events = new ArrayList<>(
                readRows(indexKey, rowKeyFactory, JavaJmxMonitoringRepository::decodeNotificationEvent));
        events.add(new StoredRow<>(newRowId(events.stream().map(StoredRow::id).toList()), event));
        int maxEvents = readNotificationSubscription(event.subscriptionId())
                .map(JmxNotificationSubscription::maxEvents)
                .orElse(DEFAULT_MAX_NOTIFICATION_EVENTS);
        writeRows(indexKey, rowKeyFactory, trimToNewest(events, maxEvents), JavaJmxMonitoringRepository::encode);
    }

    void putRaw(String key, String value) {
        preferences.put(key, value);
    }

    List<String> rawValues() {
        try {
            List<String> values = new ArrayList<>();
            for (String key : preferences.keys()) {
                values.add(preferences.get(key, ""));
            }
            return List.copyOf(values);
        } catch (BackingStoreException exception) {
            throw new IllegalStateException("Could not read preference values.", exception);
        }
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

    private List<String> readEntryIds(String prefixKey) {
        List<String> ids = new ArrayList<>();
        List<String> chunkKeys = entryChunkKeys(prefixKey);
        if (chunkKeys.isEmpty()) {
            return readIds(prefixKey);
        }
        for (String chunkKey : chunkKeys) {
            for (String id : readIds(chunkKey)) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private void writeEntryIds(String prefixKey, List<String> ids) {
        removeEntryIds(prefixKey);
        List<String> chunk = new ArrayList<>();
        int chunkLength = 0;
        int chunkIndex = 0;
        for (String id : ids) {
            String encodedId = encodeField(id);
            int nextLength = chunk.isEmpty()
                    ? encodedId.length()
                    : chunkLength + ENTRY_SEPARATOR.length() + encodedId.length();
            if (!chunk.isEmpty() && nextLength > MAX_INDEX_CHUNK_LENGTH) {
                writeIds(entryChunkKey(prefixKey, chunkIndex), chunk);
                chunkIndex++;
                chunk = new ArrayList<>();
                chunkLength = 0;
            }
            chunk.add(id);
            chunkLength = chunk.isEmpty()
                    ? encodedId.length()
                    : Math.max(encodedId.length(), nextLength);
        }
        if (!chunk.isEmpty()) {
            writeIds(entryChunkKey(prefixKey, chunkIndex), chunk);
            chunkIndex++;
        }
        preferences.putInt(entryChunkCountKey(prefixKey), chunkIndex);
    }

    private void removeEntryIds(String prefixKey) {
        preferences.remove(prefixKey);
        int chunkCount = preferences.getInt(entryChunkCountKey(prefixKey), 0);
        for (int index = 0; index < chunkCount; index++) {
            preferences.remove(entryChunkKey(prefixKey, index));
        }
        preferences.remove(entryChunkCountKey(prefixKey));
    }

    private List<String> entryChunkKeys(String prefixKey) {
        int chunkCount = preferences.getInt(entryChunkCountKey(prefixKey), 0);
        List<String> keys = new ArrayList<>();
        for (int index = 0; index < chunkCount; index++) {
            keys.add(entryChunkKey(prefixKey, index));
        }
        return keys;
    }

    private <T> List<StoredRow<T>> readRows(String indexPrefix, RowKeyFactory rowKeyFactory, Decoder<T> decoder) {
        List<StoredRow<T>> rows = new ArrayList<>();
        for (String rowId : readEntryIds(indexPrefix)) {
            String persisted = preferences.get(rowKeyFactory.key(rowId), "");
            if (!persisted.isBlank()) {
                decoder.decode(persisted).ifPresent(value -> rows.add(new StoredRow<>(rowId, value)));
            }
        }
        return List.copyOf(rows);
    }

    private <T> void writeRows(
            String indexPrefix,
            RowKeyFactory rowKeyFactory,
            List<StoredRow<T>> rows,
            Encoder<T> encoder) {
        List<String> oldIds = readEntryIds(indexPrefix);
        List<String> newIds = rows.stream()
                .map(StoredRow::id)
                .toList();
        for (String oldId : oldIds) {
            if (!newIds.contains(oldId)) {
                preferences.remove(rowKeyFactory.key(oldId));
            }
        }
        for (StoredRow<T> row : rows) {
            preferences.put(rowKeyFactory.key(row.id()), encoder.encode(row.value()));
        }
        writeEntryIds(indexPrefix, newIds);
    }

    private void removeRows(String indexPrefix, RowKeyFactory rowKeyFactory) {
        for (String rowId : readEntryIds(indexPrefix)) {
            preferences.remove(rowKeyFactory.key(rowId));
        }
        removeEntryIds(indexPrefix);
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
                    parseBoolean(decodeField(fields[8])),
                    parseBoolean(decodeField(fields[9]))));
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
                    parseBoolean(decodeField(fields[5]))));
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
                    parseBoolean(decodeField(fields[5])),
                    parseBoolean(decodeField(fields[6]))));
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

    private static String sampleIndexPrefix(String subscriptionId) {
        return SAMPLE_PREFIX + subscriptionId + ".ids";
    }

    private static String sampleRowKey(String subscriptionId, String rowId) {
        return SAMPLE_PREFIX + subscriptionId + "." + rowId;
    }

    private static String notificationKey(String id) {
        return NOTIFICATION_PREFIX + id;
    }

    private static String notificationEventIndexPrefix(String subscriptionId) {
        return NOTIFICATION_EVENT_PREFIX + subscriptionId + ".ids";
    }

    private static String notificationEventRowKey(String subscriptionId, String rowId) {
        return NOTIFICATION_EVENT_PREFIX + subscriptionId + "." + rowId;
    }

    private static String encodeField(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String entryChunkCountKey(String prefixKey) {
        return prefixKey + ENTRY_CHUNK_COUNT_SUFFIX;
    }

    private static String entryChunkKey(String prefixKey, int chunkIndex) {
        return prefixKey + ENTRY_CHUNK_SUFFIX + chunkIndex;
    }

    private static boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Malformed boolean field: " + value);
    }

    private static String newRowId(List<String> existingIds) {
        String rowId;
        do {
            rowId = Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits(), Character.MAX_RADIX);
        } while (existingIds.contains(rowId));
        return rowId;
    }

    @FunctionalInterface
    private interface Decoder<T> {
        Optional<T> decode(String entry);
    }

    @FunctionalInterface
    private interface Encoder<T> {
        String encode(T value);
    }

    @FunctionalInterface
    private interface RowKeyFactory {
        String key(String rowId);
    }

    private record StoredRow<T>(String id, T value) {
    }

    private static final class InMemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();

        private InMemoryPreferences() {
            super(null, "");
        }

        @Override
        protected void putSpi(String key, String value) {
            if (value.length() > Preferences.MAX_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                        "Preference value length exceeds " + Preferences.MAX_VALUE_LENGTH + " characters.");
            }
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
