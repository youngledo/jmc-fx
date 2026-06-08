package io.github.youngledo.jmcfx.domain.model.ai;

public record AiSettings(
        boolean enabled,
        String baseUrl,
        String model,
        double temperature,
        int maxOutputTokens,
        boolean saveApiKeyLocally) {

    public AiSettings {
        baseUrl = baseUrl == null ? "" : baseUrl;
        model = model == null ? "" : model;
        temperature = Math.clamp(temperature, 0.0, 2.0);
        maxOutputTokens = Math.max(0, maxOutputTokens);
    }
}
