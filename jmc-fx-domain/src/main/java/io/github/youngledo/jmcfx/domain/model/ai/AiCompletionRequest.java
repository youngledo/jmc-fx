package io.github.youngledo.jmcfx.domain.model.ai;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public record AiCompletionRequest(
        RecordingSummary recording,
        String languageTag,
        String prompt) {

    public AiCompletionRequest {
        if (recording == null) {
            throw new NullPointerException("recording");
        }
        languageTag = languageTag == null || languageTag.isBlank() ? "en" : languageTag;
        prompt = prompt == null ? "" : prompt;
    }
}
