package io.github.youngledo.jmcfx.adapter.ai;

import java.time.Duration;

public record OpenAiCompatibleAiSettings(
        String apiKey,
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens,
        Duration timeout) {

    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "";
    public static final double DEFAULT_TEMPERATURE = 0.2;
    public static final int DEFAULT_MAX_OUTPUT_TOKENS = 4_096;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    public OpenAiCompatibleAiSettings {
        apiKey = apiKey == null ? "" : apiKey.trim();
        baseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : stripTrailingSlash(baseUrl.trim());
        model = model == null ? DEFAULT_MODEL : model.trim();
        temperature = Math.clamp(temperature, 0.0, 2.0);
        maxOutputTokens = maxOutputTokens <= 0 ? DEFAULT_MAX_OUTPUT_TOKENS : maxOutputTokens;
        timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? DEFAULT_TIMEOUT : timeout;
    }

    public static OpenAiCompatibleAiSettings defaults() {
        return new OpenAiCompatibleAiSettings("", DEFAULT_BASE_URL, DEFAULT_MODEL,
                DEFAULT_TEMPERATURE, DEFAULT_MAX_OUTPUT_TOKENS, DEFAULT_TIMEOUT);
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
