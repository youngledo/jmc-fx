package io.github.youngledo.jmcfx.application.ai;

import java.util.Map;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;

public final class AiSettingsUseCase {

    public static final String API_KEY_ENVIRONMENT_VARIABLE = "OPENAI_API_KEY";
    public static final String MODEL_ENVIRONMENT_VARIABLE = "OPENAI_MODEL";
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final double DEFAULT_TEMPERATURE = 0.2;
    public static final int DEFAULT_MAX_OUTPUT_TOKENS = 4_096;

    private final AiSettingsRepository repository;
    private final Map<String, String> environment;

    public AiSettingsUseCase(AiSettingsRepository repository) {
        this(repository, System.getenv());
    }

    public AiSettingsUseCase(AiSettingsRepository repository, Map<String, String> environment) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
    }

    public AiSettings load() {
        return repository.load().orElseGet(AiSettingsUseCase::defaults);
    }

    public void save(AiSettings settings) {
        repository.save(Objects.requireNonNull(settings, "settings"));
    }

    public boolean apiKeyEnvironmentConfigured() {
        return !environment.getOrDefault(API_KEY_ENVIRONMENT_VARIABLE, "").isBlank();
    }

    public boolean providerConfigured() {
        AiSettings settings = load();
        return settings.enabled()
                && apiKeyEnvironmentConfigured()
                && (!settings.model().isBlank()
                        || !environment.getOrDefault(MODEL_ENVIRONMENT_VARIABLE, "").isBlank());
    }

    public static AiSettings defaults() {
        return new AiSettings(true, DEFAULT_BASE_URL, "", DEFAULT_TEMPERATURE, DEFAULT_MAX_OUTPUT_TOKENS, false);
    }
}
