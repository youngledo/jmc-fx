package io.github.youngledo.jmcfx.adapter.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import io.github.youngledo.jmcfx.domain.model.SavedJvmTarget;
import org.junit.jupiter.api.Test;

class JavaSavedJvmTargetRepositoryTest {

    private static final String LEGACY_NODE_PATH = "/com/youngledo/jmcfx/ui/preferences";

    @Test
    void storesUpdatesAndDeletesTargets() {
        JavaSavedJvmTargetRepository repository = JavaSavedJvmTargetRepository.inMemory();
        SavedJvmTarget first = new SavedJvmTarget("alpha", "Alpha", "service:jmx:rmi:///jndi/rmi://localhost:7001/jmxrmi", null);
        SavedJvmTarget second = new SavedJvmTarget("beta", "Beta", "service:jmx:rmi:///jndi/rmi://localhost:7002/jmxrmi", null);

        repository.save(first);
        repository.save(second);
        repository.save(new SavedJvmTarget("alpha", "Alpha Updated",
                "service:jmx:rmi:///jndi/rmi://localhost:17001/jmxrmi", null));
        repository.markConnected("alpha", Instant.parse("2026-05-28T08:15:30Z"));
        repository.deleteById("beta");

        assertEquals(List.of(new SavedJvmTarget("alpha", "Alpha Updated",
                "service:jmx:rmi:///jndi/rmi://localhost:17001/jmxrmi",
                Instant.parse("2026-05-28T08:15:30Z"))), repository.findAll());
    }

    @Test
    void ignoresMalformedPerTargetEntries() {
        JavaSavedJvmTargetRepository repository = JavaSavedJvmTargetRepository.inMemory();
        repository.putRaw("targetIds", encoded("bad") + "\n" + encoded("valid"));
        repository.putRaw(targetKey("bad"), "not-base64|fields");
        repository.putRaw(targetKey("valid"), encodedEntry(
                "valid", "Valid", "service:jmx:rmi:///valid", ""));

        assertEquals(List.of(new SavedJvmTarget("valid", "Valid", "service:jmx:rmi:///valid", null)),
                repository.findAll());
    }

    @Test
    void sortsByDisplayNameCaseInsensitiveThenServiceUrl() {
        JavaSavedJvmTargetRepository repository = JavaSavedJvmTargetRepository.inMemory();

        repository.save(new SavedJvmTarget("z", "Zulu", "service:jmx:rmi:///zulu", null));
        repository.save(new SavedJvmTarget("a2", "alpha", "service:jmx:rmi:///b", null));
        repository.save(new SavedJvmTarget("a1", "Alpha", "service:jmx:rmi:///a", null));

        assertEquals(List.of(
                new SavedJvmTarget("a1", "Alpha", "service:jmx:rmi:///a", null),
                new SavedJvmTarget("a2", "alpha", "service:jmx:rmi:///b", null),
                new SavedJvmTarget("z", "Zulu", "service:jmx:rmi:///zulu", null)), repository.findAll());
    }

    @Test
    void assignsStableIdForBlankId() {
        JavaSavedJvmTargetRepository firstRepository = JavaSavedJvmTargetRepository.inMemory();
        JavaSavedJvmTargetRepository secondRepository = JavaSavedJvmTargetRepository.inMemory();
        SavedJvmTarget target = new SavedJvmTarget("", "Local Process", "service:jmx:rmi:///stable", null);

        SavedJvmTarget first = firstRepository.save(target);
        SavedJvmTarget second = secondRepository.save(target);

        assertFalse(first.id().isBlank());
        assertEquals(first.id(), second.id());
        assertEquals(List.of(first), firstRepository.findAll());
    }

    @Test
    void rejectsBlankServiceUrl() {
        JavaSavedJvmTargetRepository repository = JavaSavedJvmTargetRepository.inMemory();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.save(new SavedJvmTarget("", "Local Process", " ", null)));

        assertEquals("Saved JVM target service URL must not be blank.", exception.getMessage());
    }

    @Test
    void storesManyLongUrlsWithoutSingleOversizedPreferenceValue() {
        JavaSavedJvmTargetRepository repository = new JavaSavedJvmTargetRepository();
        String prefix = "jmc-fx-test-" + UUID.randomUUID();
        List<SavedJvmTarget> targets = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            targets.add(new SavedJvmTarget(prefix + "-" + index, "Long " + index,
                    "service:jmx:rmi:///jndi/rmi://localhost:" + (17_000 + index)
                            + "/jmxrmi?token=" + "x".repeat(850),
                    null));
        }

        try {
            for (SavedJvmTarget target : targets) {
                repository.save(target);
            }

            assertEquals(targets, repository.findAll().stream()
                    .filter(target -> target.id().startsWith(prefix))
                    .toList());
        } finally {
            for (SavedJvmTarget target : targets) {
                repository.deleteById(target.id());
            }
        }
    }

    @Test
    void savesFindsAndDeletesExplicitLongId() {
        JavaSavedJvmTargetRepository repository = new JavaSavedJvmTargetRepository();
        SavedJvmTarget target = new SavedJvmTarget("target-" + "x".repeat(200), "Long Id",
                "service:jmx:rmi:///long-id-" + UUID.randomUUID(), null);

        try {
            assertEquals(target, repository.save(target));
            assertEquals(List.of(target), repository.findAll().stream()
                    .filter(saved -> saved.id().equals(target.id()))
                    .toList());

            repository.deleteById(target.id());

            assertEquals(List.of(), repository.findAll().stream()
                    .filter(saved -> saved.id().equals(target.id()))
                    .toList());
        } finally {
            repository.deleteById(target.id());
        }
    }

    @Test
    void defaultRepositoryUsesLegacyUiPreferenceNodeAfterAdapterMove() throws BackingStoreException {
        Preferences legacyNode = legacyNode();
        String id = "legacy-node-" + UUID.randomUUID();
        SavedJvmTarget target = new SavedJvmTarget(id, "Legacy Node",
                "service:jmx:rmi:///legacy-node-" + UUID.randomUUID(), null);

        try {
            new JavaSavedJvmTargetRepository().save(target);

            assertEquals(encodedEntry(target.id(), target.displayName(), target.serviceUrl(), ""),
                    legacyNode.get(targetKey(id), ""));
        } finally {
            new JavaSavedJvmTargetRepository().deleteById(id);
            legacyNode.remove(targetKey(id));
        }
    }

    private static String encodedEntry(String id, String displayName, String serviceUrl, String lastConnectedAt) {
        return String.join("|", encoded(id), encoded(displayName), encoded(serviceUrl), encoded(lastConnectedAt));
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String targetKey(String id) {
        return "target." + UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
    }

    private static Preferences legacyNode() throws BackingStoreException {
        return Preferences.userRoot().node(LEGACY_NODE_PATH);
    }
}
