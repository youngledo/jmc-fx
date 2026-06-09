package io.github.youngledo.jmcfx.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;
import org.junit.jupiter.api.Test;

class AiSettingsUseCaseTest {

    @Test
    void loadsDefaultsWhenNothingWasSaved() {
        AiSettingsUseCase useCase = new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.empty()), Map.of());

        AiSettings settings = useCase.load();

        assertTrue(settings.enabled());
        assertEquals(AiSettingsUseCase.DEFAULT_BASE_URL, settings.baseUrl());
        assertEquals("", settings.model());
        assertEquals(AiSettingsUseCase.DEFAULT_TEMPERATURE, settings.temperature());
        assertEquals(AiSettingsUseCase.DEFAULT_MAX_OUTPUT_TOKENS, settings.maxOutputTokens());
        assertFalse(settings.saveApiKeyLocally());
    }

    @Test
    void savesNonSecretSettingsThroughRepository() {
        FakeAiSettingsRepository repository = new FakeAiSettingsRepository(Optional.empty());
        AiSettings settings = new AiSettings(false, "https://ai.example/v1", "demo", 0.7, 2_048, false);
        AiSettingsUseCase useCase = new AiSettingsUseCase(repository, Map.of());

        useCase.save(settings);

        assertEquals(settings, repository.load().orElseThrow());
    }

    @Test
    void reportsWhetherOpenAiApiKeyExistsInEnvironment() {
        AiSettingsUseCase configured = new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.empty()),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "test-key"));
        AiSettingsUseCase missing = new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.empty()),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, " "));

        assertTrue(configured.apiKeyEnvironmentConfigured());
        assertFalse(missing.apiKeyEnvironmentConfigured());
    }

    @Test
    void reportsProviderConfiguredOnlyWhenEnabledKeyAndModelAreAvailable() {
        assertTrue(new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "saved-model", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key")).providerConfigured());
        assertTrue(new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "", 0.2, 4_096, false))),
                Map.of(
                        AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key",
                        AiSettingsUseCase.MODEL_ENVIRONMENT_VARIABLE, "env-model")).providerConfigured());
        assertFalse(new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(false, "https://api.openai.com/v1", "saved-model", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key")).providerConfigured());
        assertFalse(new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "saved-model", 0.2, 4_096, false))),
                Map.of()).providerConfigured());
        assertFalse(new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key")).providerConfigured());
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
