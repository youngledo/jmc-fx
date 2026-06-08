package io.github.youngledo.jmcfx.domain.model.ai;

public record AiCompletionResponse(String text) {
    public AiCompletionResponse {
        text = text == null ? "" : text;
    }
}
