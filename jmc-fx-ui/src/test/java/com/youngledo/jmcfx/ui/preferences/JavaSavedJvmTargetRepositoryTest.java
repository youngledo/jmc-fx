package com.youngledo.jmcfx.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import org.junit.jupiter.api.Test;

class JavaSavedJvmTargetRepositoryTest {

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
    void ignoresMalformedEntries() {
        JavaSavedJvmTargetRepository repository = JavaSavedJvmTargetRepository.inMemory();
        repository.save(new SavedJvmTarget("valid", "Valid", "service:jmx:rmi:///valid", null));
        repository.putRaw("targets", "not-base64|fields\n" + encodedEntry(
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

    private static String encodedEntry(String id, String displayName, String serviceUrl, String lastConnectedAt) {
        return String.join("|", encoded(id), encoded(displayName), encoded(serviceUrl), encoded(lastConnectedAt));
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
