package io.github.youngledo.jmcfx.adapter.preferences.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import org.junit.jupiter.api.Test;

class JavaAiSettingsRepositoryTest {

    @Test
    void startsEmptyWhenNoSettingsWereSaved() {
        JavaAiSettingsRepository repository = JavaAiSettingsRepository.inMemory();

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void savesAndLoadsNonSecretAiSettings() {
        JavaAiSettingsRepository repository = JavaAiSettingsRepository.inMemory();
        var settings = new AiSettings(true, "https://example.test/v1", "test-model", 0.7, 2_048, false);

        repository.save(settings);

        assertEquals(settings, repository.load().orElseThrow());
    }

    @Test
    void normalizesInvalidSettingsThroughDomainModel() {
        JavaAiSettingsRepository repository = JavaAiSettingsRepository.inMemory();

        repository.save(new AiSettings(true, "", "test-model", 8.0, -1, true));

        AiSettings loaded = repository.load().orElseThrow();
        assertEquals(2.0, loaded.temperature());
        assertEquals(0, loaded.maxOutputTokens());
    }

    @Test
    void loadsSavedDisabledSettingsEvenWhenProviderFieldsAreBlank() {
        JavaAiSettingsRepository repository = JavaAiSettingsRepository.inMemory();

        repository.save(new AiSettings(false, "", "", 0.2, 4_096, false));

        assertEquals(new AiSettings(false, "", "", 0.2, 4_096, false), repository.load().orElseThrow());
    }
}
