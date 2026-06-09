package io.github.youngledo.jmcfx.adapter.ai;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class EnvironmentOpenAiCompatibleAiSettingsProvider implements OpenAiCompatibleAiSettingsProvider {

    public static final String OPENAI_API_KEY = "OPENAI_API_KEY";
    public static final String OPENAI_BASE_URL = "OPENAI_BASE_URL";
    public static final String OPENAI_MODEL = "OPENAI_MODEL";
    public static final String OPENAI_TIMEOUT_SECONDS = "OPENAI_TIMEOUT_SECONDS";

    private final Map<String, String> environment;

    public EnvironmentOpenAiCompatibleAiSettingsProvider() {
        this(System.getenv());
    }

    EnvironmentOpenAiCompatibleAiSettingsProvider(Map<String, String> environment) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
    }

    @Override
    public OpenAiCompatibleAiSettings load() {
        OpenAiCompatibleAiSettings defaults = OpenAiCompatibleAiSettings.defaults();
        return new OpenAiCompatibleAiSettings(
                environment.getOrDefault(OPENAI_API_KEY, ""),
                environment.getOrDefault(OPENAI_BASE_URL, defaults.baseUrl()),
                environment.getOrDefault(OPENAI_MODEL, defaults.model()),
                defaults.temperature(),
                defaults.maxOutputTokens(),
                timeout(defaults.timeout()));
    }

    private Duration timeout(Duration fallback) {
        String value = environment.getOrDefault(OPENAI_TIMEOUT_SECONDS, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            long seconds = Long.parseLong(value.strip());
            return seconds <= 0 ? fallback : Duration.ofSeconds(seconds);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
