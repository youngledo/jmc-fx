package com.youngledo.jmcfx.ui.preferences;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;

import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import com.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;

public final class JavaSavedJvmTargetRepository implements SavedJvmTargetRepository {

    private static final String TARGET_IDS = "targetIds";
    private static final String TARGET_PREFIX = "target.";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String ENTRY_SEPARATOR = "\n";
    private static final String BLANK_SERVICE_URL_MESSAGE = "Saved JVM target service URL must not be blank.";
    private static final Comparator<SavedJvmTarget> DISPLAY_ORDER = Comparator
            .comparing((SavedJvmTarget target) -> target.displayName().toLowerCase(Locale.ROOT))
            .thenComparing(SavedJvmTarget::serviceUrl);

    private final Preferences preferences;

    public JavaSavedJvmTargetRepository() {
        this(Preferences.userNodeForPackage(JavaSavedJvmTargetRepository.class));
    }

    private JavaSavedJvmTargetRepository(Preferences preferences) {
        this.preferences = preferences;
    }

    public static JavaSavedJvmTargetRepository inMemory() {
        return new JavaSavedJvmTargetRepository(new InMemoryPreferences());
    }

    @Override
    public List<SavedJvmTarget> findAll() {
        return readTargets().stream()
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    @Override
    public SavedJvmTarget save(SavedJvmTarget target) {
        Objects.requireNonNull(target, "target");
        if (target.serviceUrl().isBlank()) {
            throw new IllegalArgumentException(BLANK_SERVICE_URL_MESSAGE);
        }
        SavedJvmTarget targetToSave = target.id().isBlank()
                ? new SavedJvmTarget(stableId(target.serviceUrl()), target.displayName(), target.serviceUrl(),
                        target.lastConnectedAt())
                : target;
        List<String> ids = readIds();
        if (!ids.contains(targetToSave.id())) {
            ids.add(targetToSave.id());
            writeIds(ids);
        }
        writeTarget(targetToSave);
        return targetToSave;
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        List<String> ids = readIds();
        ids.removeIf(existing -> existing.equals(id));
        writeIds(ids);
        preferences.remove(targetKey(id));
    }

    @Override
    public void markConnected(String id, Instant connectedAt) {
        if (id == null || id.isBlank()) {
            return;
        }
        readTarget(id).ifPresent(target -> writeTarget(target.withLastConnectedAt(connectedAt)));
    }

    void putRaw(String key, String value) {
        preferences.put(key, value);
    }

    private List<SavedJvmTarget> readTargets() {
        List<SavedJvmTarget> targets = new ArrayList<>();
        for (String id : readIds()) {
            readTarget(id).ifPresent(targets::add);
        }
        return targets;
    }

    private List<String> readIds() {
        String persisted = preferences.get(TARGET_IDS, "");
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

    private void writeIds(List<String> ids) {
        preferences.put(TARGET_IDS, ids.stream()
                .map(JavaSavedJvmTargetRepository::encodeField)
                .reduce((first, second) -> first + ENTRY_SEPARATOR + second)
                .orElse(""));
    }

    private Optional<SavedJvmTarget> readTarget(String id) {
        String persisted = preferences.get(targetKey(id), "");
        if (persisted.isBlank()) {
            return Optional.empty();
        }
        return decode(persisted);
    }

    private void writeTarget(SavedJvmTarget target) {
        preferences.put(targetKey(target.id()), encode(target));
    }

    private static String targetKey(String id) {
        return TARGET_PREFIX + encodeField(id);
    }

    private static Optional<SavedJvmTarget> decode(String entry) {
        String[] fields = entry.split(FIELD_SEPARATOR, -1);
        if (fields.length != 4) {
            return Optional.empty();
        }
        try {
            String id = decodeField(fields[0]);
            String displayName = decodeField(fields[1]);
            String serviceUrl = decodeField(fields[2]);
            String lastConnectedAt = decodeField(fields[3]);
            return Optional.of(new SavedJvmTarget(id, displayName, serviceUrl,
                    lastConnectedAt.isBlank() ? null : Instant.parse(lastConnectedAt)));
        } catch (IllegalArgumentException | DateTimeException exception) {
            return Optional.empty();
        }
    }

    private static String encode(SavedJvmTarget target) {
        return String.join("|",
                encodeField(target.id()),
                encodeField(target.displayName()),
                encodeField(target.serviceUrl()),
                encodeField(target.lastConnectedAt() == null ? "" : target.lastConnectedAt().toString()));
    }

    private static String encodeField(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String stableId(String serviceUrl) {
        return UUID.nameUUIDFromBytes(serviceUrl.getBytes(StandardCharsets.UTF_8)).toString();
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
