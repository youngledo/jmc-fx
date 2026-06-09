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
            throw new JmcFxException(httpFailureMessage(response));
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
        ProviderResponse providerResponse = streamOrFallback(request, settings, requestBody, listener);
        if (providerResponse.response().statusCode() < 200 || providerResponse.response().statusCode() >= 300) {
            throw new JmcFxException(httpFailureMessage(providerResponse.response()));
        }
        if (providerResponse.streaming()) {
            return new AiCompletionResponse(providerResponse.response().body());
        }
        return new AiCompletionResponse(extractText(providerResponse.response().body()));
    }

    private ProviderResponse streamOrFallback(AiCompletionRequest request,
            OpenAiCompatibleAiSettings settings, String requestBody, AiCompletionStreamListener listener) {
        try {
            var response = transport.postStream(chatCompletionsUri(settings), headers(settings), requestBody,
                    settings.timeout(), contentDelta -> {
                        if (listener != null) {
                            listener.onContentDelta(contentDelta);
                        }
                    });
            return new ProviderResponse(response, true);
        } catch (JmcFxException exception) {
            String fallbackBody = requestBody(settings, request, false);
            var response = transport.post(chatCompletionsUri(settings), headers(settings), fallbackBody,
                    settings.timeout());
            return new ProviderResponse(response, false);
        }
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
                + "Follow the response format requested by the user prompt exactly.");

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

    private static String httpFailureMessage(OpenAiCompatibleHttpTransport.HttpResponse response) {
        String detail = providerErrorDetail(response.body());
        if (detail.isBlank()) {
            return "AI provider request failed with HTTP " + response.statusCode() + ".";
        }
        return "AI provider request failed with HTTP " + response.statusCode() + ": " + detail + ".";
    }

    private static String providerErrorDetail(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode error = MAPPER.readTree(responseBody).path("error");
            String message = error.path("message").asText("");
            String type = error.path("type").asText("");
            String code = error.path("code").asText("");
            if (message.isBlank()) {
                return "";
            }
            if (!type.isBlank() && !code.isBlank()) {
                return message + " (" + type + "/" + code + ")";
            }
            if (!type.isBlank()) {
                return message + " (" + type + ")";
            }
            if (!code.isBlank()) {
                return message + " (" + code + ")";
            }
            return message;
        } catch (JsonProcessingException exception) {
            return "";
        }
    }

    private record ProviderResponse(OpenAiCompatibleHttpTransport.HttpResponse response, boolean streaming) {
    }

}
