package io.github.youngledo.jmcfx.adapter.ai;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionStreamListener;
import io.github.youngledo.jmcfx.domain.service.ai.StreamingAiCompletionService;

public final class OpenAiCompatibleCompletionService implements StreamingAiCompletionService {

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

        String requestBody = requestBody(settings, request, false);
        var response = transport.post(chatCompletionsUri(settings), headers(settings), requestBody, settings.timeout());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new JmcFxException("AI provider request failed with HTTP " + response.statusCode() + ".");
        }
        return new AiCompletionResponse(extractText(response.body()));
    }

    @Override
    public AiCompletionResponse completeStreaming(AiCompletionRequest request, AiCompletionStreamListener listener) {
        Objects.requireNonNull(request, "request");
        OpenAiCompatibleAiSettings settings = settingsProvider.load();
        if (settings.apiKey().isBlank()) {
            throw new JmcFxException("AI provider API key is not configured.");
        }
        if (settings.model().isBlank()) {
            throw new JmcFxException("AI provider model is not configured.");
        }

        String requestBody = requestBody(settings, request, true);
        var response = transport.postStream(chatCompletionsUri(settings), headers(settings), requestBody,
                settings.timeout(), contentDelta -> {
                    if (listener != null) {
                        listener.onContentDelta(contentDelta);
                    }
                });
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new JmcFxException("AI provider request failed with HTTP " + response.statusCode() + ".");
        }
        return new AiCompletionResponse(response.body());
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

    private static String requestBody(OpenAiCompatibleAiSettings settings, AiCompletionRequest request,
            boolean stream) {
        var body = MAPPER.createObjectNode();
        body.put("model", settings.model());
        body.put("temperature", settings.temperature());
        body.put("max_completion_tokens", settings.maxOutputTokens());
        body.put("stream", stream);

        var messages = body.putArray("messages");
        var systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are the JMC FX offline recording assistant. "
                + "Return only the JSON requested by the user prompt.");

        var userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", request.prompt());

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

}
