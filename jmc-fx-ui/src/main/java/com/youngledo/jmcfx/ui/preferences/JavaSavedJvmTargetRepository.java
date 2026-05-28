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

    private static final String TARGETS = "targets";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String ENTRY_SEPARATOR = "\n";
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
        SavedJvmTarget targetToSave = target.id().isBlank()
                ? new SavedJvmTarget(stableId(target.serviceUrl()), target.displayName(), target.serviceUrl(),
                        target.lastConnectedAt())
                : target;
        List<SavedJvmTarget> targets = readTargets();
        targets.removeIf(existing -> existing.id().equals(targetToSave.id()));
        targets.add(targetToSave);
        writeTargets(targets);
        return targetToSave;
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        List<SavedJvmTarget> targets = readTargets();
        targets.removeIf(target -> target.id().equals(id));
        writeTargets(targets);
    }

    @Override
    public void markConnected(String id, Instant connectedAt) {
        if (id == null || id.isBlank()) {
            return;
        }
        List<SavedJvmTarget> targets = readTargets();
        boolean changed = false;
        for (int index = 0; index < targets.size(); index++) {
            SavedJvmTarget target = targets.get(index);
            if (target.id().equals(id)) {
                targets.set(index, target.withLastConnectedAt(connectedAt));
                changed = true;
            }
        }
        if (changed) {
            writeTargets(targets);
        }
    }

    void putRaw(String key, String value) {
        preferences.put(key, value);
    }

    private List<SavedJvmTarget> readTargets() {
        String persisted = preferences.get(TARGETS, "");
        if (persisted.isBlank()) {
            return new ArrayList<>();
        }
        List<SavedJvmTarget> targets = new ArrayList<>();
        for (String entry : persisted.split(ENTRY_SEPARATOR)) {
            decode(entry).ifPresent(targets::add);
        }
        return targets;
    }

    private void writeTargets(List<SavedJvmTarget> targets) {
        preferences.put(TARGETS, targets.stream()
                .map(JavaSavedJvmTargetRepository::encode)
                .reduce((first, second) -> first + ENTRY_SEPARATOR + second)
                .orElse(""));
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
