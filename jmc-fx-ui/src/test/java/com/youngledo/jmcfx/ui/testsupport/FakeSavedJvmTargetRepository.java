package com.youngledo.jmcfx.ui.testsupport;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import com.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;

public class FakeSavedJvmTargetRepository implements SavedJvmTargetRepository {

    private static final String BLANK_SERVICE_URL_MESSAGE = "Saved JVM target service URL must not be blank.";
    private static final Comparator<SavedJvmTarget> DISPLAY_ORDER = Comparator
            .comparing((SavedJvmTarget target) -> target.displayName().toLowerCase(Locale.ROOT))
            .thenComparing(SavedJvmTarget::serviceUrl);

    private final Map<String, SavedJvmTarget> targets = new HashMap<>();

    @Override
    public List<SavedJvmTarget> findAll() {
        return targets.values().stream()
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
        targets.put(targetToSave.id(), targetToSave);
        return targetToSave;
    }

    @Override
    public void deleteById(String id) {
        if (id != null && !id.isBlank()) {
            targets.remove(id);
        }
    }

    @Override
    public void markConnected(String id, Instant connectedAt) {
        if (id == null || id.isBlank()) {
            return;
        }
        SavedJvmTarget target = targets.get(id);
        if (target != null) {
            targets.put(id, target.withLastConnectedAt(connectedAt));
        }
    }

    private static String stableId(String serviceUrl) {
        return UUID.nameUUIDFromBytes(serviceUrl.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
