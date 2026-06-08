package io.github.youngledo.jmcfx.adapter.ai;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;

public final class RepositoryOpenAiCompatibleAiSettingsProvider implements OpenAiCompatibleAiSettingsProvider {

    private final AiSettingsRepository repository;
    private final OpenAiCompatibleAiSettingsProvider fallback;

    public RepositoryOpenAiCompatibleAiSettingsProvider(AiSettingsRepository repository) {
        this(repository, new EnvironmentOpenAiCompatibleAiSettingsProvider());
    }

    public RepositoryOpenAiCompatibleAiSettingsProvider(AiSettingsRepository repository,
            OpenAiCompatibleAiSettingsProvider fallback) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public OpenAiCompatibleAiSettings load() {
        OpenAiCompatibleAiSettings fallbackSettings = fallback.load();
        return repository.load()
                .map(settings -> merge(settings, fallbackSettings))
                .orElse(fallbackSettings);
    }

    private static OpenAiCompatibleAiSettings merge(AiSettings settings,
            OpenAiCompatibleAiSettings fallbackSettings) {
        return new OpenAiCompatibleAiSettings(
                fallbackSettings.apiKey(),
                settings.baseUrl().isBlank() ? fallbackSettings.baseUrl() : settings.baseUrl(),
                settings.model().isBlank() ? fallbackSettings.model() : settings.model(),
                settings.temperature(),
                settings.maxOutputTokens(),
                fallbackSettings.timeout());
    }
}
