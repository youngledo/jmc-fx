package io.github.youngledo.jmcfx.adapter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Optional;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;
import org.junit.jupiter.api.Test;

class RepositoryOpenAiCompatibleAiSettingsProviderTest {

    @Test
    void savedSettingsOverrideNonSecretFallbackSettings() {
        var repository = new FakeAiSettingsRepository(Optional.of(new AiSettings(
                true, "https://saved.test/v1", "saved-model", 0.6, 2_000, false)));
        var fallback = new OpenAiCompatibleAiSettings("env-key", "https://env.test/v1", "env-model",
                0.2, 4_096, Duration.ofSeconds(9));
        var provider = new RepositoryOpenAiCompatibleAiSettingsProvider(repository, () -> fallback);

        OpenAiCompatibleAiSettings settings = provider.load();

        assertEquals("env-key", settings.apiKey());
        assertEquals("https://saved.test/v1", settings.baseUrl());
        assertEquals("saved-model", settings.model());
        assertEquals(0.6, settings.temperature());
        assertEquals(2_000, settings.maxOutputTokens());
        assertEquals(Duration.ofSeconds(9), settings.timeout());
    }

    @Test
    void blankSavedBaseUrlAndModelKeepFallbackValues() {
        var repository = new FakeAiSettingsRepository(Optional.of(new AiSettings(
                true, "", "", 0.9, 1_000, false)));
        var fallback = new OpenAiCompatibleAiSettings("env-key", "https://env.test/v1", "env-model",
                0.2, 4_096, Duration.ofSeconds(9));
        var provider = new RepositoryOpenAiCompatibleAiSettingsProvider(repository, () -> fallback);

        OpenAiCompatibleAiSettings settings = provider.load();

        assertEquals("https://env.test/v1", settings.baseUrl());
        assertEquals("env-model", settings.model());
        assertEquals(0.9, settings.temperature());
        assertEquals(1_000, settings.maxOutputTokens());
    }

    @Test
    void usesFallbackWhenNoSettingsWereSaved() {
        var fallback = new OpenAiCompatibleAiSettings("env-key", "https://env.test/v1", "env-model",
                0.2, 4_096, Duration.ofSeconds(9));
        var provider = new RepositoryOpenAiCompatibleAiSettingsProvider(new FakeAiSettingsRepository(Optional.empty()),
                () -> fallback);

        assertEquals(fallback, provider.load());
    }

    private static final class FakeAiSettingsRepository implements AiSettingsRepository {

        private Optional<AiSettings> settings;

        private FakeAiSettingsRepository(Optional<AiSettings> settings) {
            this.settings = settings;
        }

        @Override
        public Optional<AiSettings> load() {
            return settings;
        }

        @Override
        public void save(AiSettings settings) {
            this.settings = Optional.of(settings);
        }
    }
}
