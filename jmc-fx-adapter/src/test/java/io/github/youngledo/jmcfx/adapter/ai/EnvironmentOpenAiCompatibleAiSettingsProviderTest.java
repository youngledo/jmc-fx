package io.github.youngledo.jmcfx.adapter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class EnvironmentOpenAiCompatibleAiSettingsProviderTest {

    @Test
    void loadsOpenAiEnvironmentVariables() {
        var provider = new EnvironmentOpenAiCompatibleAiSettingsProvider(Map.of(
                "OPENAI_API_KEY", "test-key",
                "OPENAI_BASE_URL", "https://example.test/v1/",
                "OPENAI_MODEL", "test-model"));

        OpenAiCompatibleAiSettings settings = provider.load();

        assertEquals("test-key", settings.apiKey());
        assertEquals("https://example.test/v1", settings.baseUrl());
        assertEquals("test-model", settings.model());
    }

    @Test
    void usesDefaultsForMissingOptionalEnvironmentVariables() {
        var provider = new EnvironmentOpenAiCompatibleAiSettingsProvider(Map.of("OPENAI_API_KEY", "test-key"));

        OpenAiCompatibleAiSettings settings = provider.load();

        assertEquals("test-key", settings.apiKey());
        assertEquals(OpenAiCompatibleAiSettings.DEFAULT_BASE_URL, settings.baseUrl());
        assertEquals(OpenAiCompatibleAiSettings.DEFAULT_MODEL, settings.model());
    }
}
