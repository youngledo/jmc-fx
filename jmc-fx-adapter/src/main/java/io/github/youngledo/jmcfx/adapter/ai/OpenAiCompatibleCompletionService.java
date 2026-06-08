package io.github.youngledo.jmcfx.adapter.ai;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;

public final class OpenAiCompatibleCompletionService implements AiCompletionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenAiCompatibleAiSettingsProvider settingsProvider;
    private final OpenAiCompatibleHttpTransport transport;

    public OpenAiCompatibleCompletionService(OpenAiCompatibleAiSettingsProvider settingsProvider) {
        this(settingsProvider, new JavaHttpClientOpenAiCompatibleHttpTransport());
    }

    OpenAiCompatibleCompletionService(OpenAiCompatibleAiSettingsProvider settingsProvider,
            OpenAiCompatibleHttpTransport transport) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    @Override
    public AiCompletionResponse complete(AiCompletionRequest request) {
        Objects.requireNonNull(request, "request");
        OpenAiCompatibleAiSettings settings = settingsProvider.load();
        if (settings.apiKey().isBlank()) {
            throw new JmcFxException("AI provider API key is not configured.");
        }
        if (settings.model().isBlank()) {
            throw new JmcFxException("AI provider model is not configured.");
        }

        String requestBody = requestBody(settings, request);
        var response = transport.post(chatCompletionsUri(settings), headers(settings), requestBody, settings.timeout());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new JmcFxException("AI provider request failed with HTTP " + response.statusCode() + ".");
        }
        return new AiCompletionResponse(extractText(response.body()));
    }

    private static URI chatCompletionsUri(OpenAiCompatibleAiSettings settings) {
        return URI.create(settings.baseUrl() + "/chat/completions");
    }

    private static Map<String, String> headers(OpenAiCompatibleAiSettings settings) {
        return Map.of(
                "Authorization", "Bearer " + settings.apiKey(),
                "Content-Type", "application/json",
                "Accept", "application/json");
    }

    private static String requestBody(OpenAiCompatibleAiSettings settings, AiCompletionRequest request) {
        var body = new ChatCompletionRequest(
                settings.model(),
                List.of(
                        new ChatMessage("system", "You are the JMC FX offline recording assistant. "
                                + "Return only the JSON requested by the user prompt."),
                        new ChatMessage("user", request.prompt())),
                settings.temperature(),
                settings.maxOutputTokens(),
                false);
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new JmcFxException("Unable to build AI provider request.", exception);
        }
    }

    private static String extractText(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new JmcFxException("AI provider response did not contain message content.");
            }
            return content.asText();
        } catch (JsonProcessingException exception) {
            throw new JmcFxException("AI provider returned invalid JSON.", exception);
        }
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            int max_completion_tokens,
            boolean stream) {
    }

    private record ChatMessage(String role, String content) {
        private ChatMessage {
            content = content == null ? "" : content;
        }
    }
}
