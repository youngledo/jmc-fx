package io.github.youngledo.jmcfx.adapter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleCompletionServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void postsOpenAiCompatibleChatCompletionRequest() throws Exception {
        FakeTransport transport = new FakeTransport(200, """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summaryMarkdown\\":\\"ok\\"}"
                      }
                    }
                  ]
                }
                """);
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1/", "test-model",
                0.4, 1234, Duration.ofSeconds(7));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        var response = service.complete(new AiCompletionRequest(recording(), "zh-CN", "Return JSON."));

        assertEquals("{\"summaryMarkdown\":\"ok\"}", response.text());
        assertEquals(URI.create("https://example.test/v1/chat/completions"), transport.uri);
        assertEquals("Bearer test-key", transport.headers.get("Authorization"));
        assertEquals("application/json", transport.headers.get("Content-Type"));
        assertEquals("application/json", transport.headers.get("Accept"));
        assertEquals(Duration.ofSeconds(7), transport.timeout);

        JsonNode body = MAPPER.readTree(transport.body);
        assertEquals("test-model", body.path("model").asText());
        assertEquals(0.4, body.path("temperature").asDouble());
        assertEquals(1234, body.path("max_completion_tokens").asInt());
        assertEquals(false, body.path("stream").asBoolean());
        assertEquals("system", body.path("messages").path(0).path("role").asText());
        assertEquals("You are the JMC FX offline recording assistant. "
                + "Follow the response format requested by the user prompt exactly.",
                body.path("messages").path(0).path("content").asText());
        assertEquals("user", body.path("messages").path(1).path("role").asText());
        assertEquals("Return JSON.", body.path("messages").path(1).path("content").asText());
    }

    @Test
    void postsStreamingChatCompletionRequestAndForwardsContentDeltas() throws Exception {
        FakeTransport transport = new FakeTransport(200, "");
        transport.streamResponseBody = """
                {"summaryMarkdown":"ok"}
                """;
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1/", "test-model",
                0.4, 1234, Duration.ofSeconds(7));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);
        StringBuilder streamedText = new StringBuilder();

        var response = service.completeStreaming(new AiCompletionRequest(recording(), "zh-CN", "Return JSON."),
                streamedText::append);

        assertEquals("{\"summaryMarkdown\":\"ok\"}", response.text());
        assertEquals("{\"summaryMarkdown\":\"ok\"}", streamedText.toString());
        assertEquals(URI.create("https://example.test/v1/chat/completions"), transport.uri);
        assertEquals(Duration.ofSeconds(7), transport.timeout);

        JsonNode body = MAPPER.readTree(transport.body);
        assertEquals(true, body.path("stream").asBoolean());
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        FakeTransport transport = new FakeTransport(200, "{}");
        var service = new OpenAiCompatibleCompletionService(OpenAiCompatibleAiSettings::defaults, transport);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.complete(new AiCompletionRequest(recording(), "en", "prompt")));

        assertEquals("AI provider API key is not configured.", exception.getMessage());
        assertEquals(null, transport.uri);
    }

    @Test
    void rejectsMissingModelBeforeSendingRequest() {
        FakeTransport transport = new FakeTransport(200, "{}");
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1", "",
                0.2, 100, Duration.ofSeconds(1));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.complete(new AiCompletionRequest(recording(), "en", "prompt")));

        assertEquals("AI provider model is not configured.", exception.getMessage());
        assertEquals(null, transport.uri);
    }

    @Test
    void mapsHttpErrorsToApplicationException() {
        FakeTransport transport = new FakeTransport(429, "{\"error\":{\"message\":\"rate limited\"}}");
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1", "test-model",
                0.2, 100, Duration.ofSeconds(1));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.complete(new AiCompletionRequest(recording(), "en", "prompt")));

        assertEquals("AI provider request failed with HTTP 429: rate limited.", exception.getMessage());
    }

    @Test
    void mapsStructuredProviderErrorsToApplicationException() {
        FakeTransport transport = new FakeTransport(503, """
                {
                  "error": {
                    "message": "No available providers",
                    "type": "no_available_providers",
                    "code": "no_available_providers"
                  }
                }
                """);
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1", "test-model",
                0.2, 100, Duration.ofSeconds(1));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.completeStreaming(new AiCompletionRequest(recording(), "en", "prompt"), ignored -> { }));

        assertEquals("AI provider request failed with HTTP 503: No available providers "
                + "(no_available_providers/no_available_providers).", exception.getMessage());
    }

    @Test
    void fallsBackToNonStreamingWhenStreamingConnectionCloses() throws Exception {
        FakeTransport transport = new FakeTransport(200, """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summaryMarkdown\\":\\"fallback\\"}"
                      }
                    }
                  ]
                }
                """);
        transport.streamFailure = new JmcFxException(
                "Unable to call AI provider at airoute.mycyjg.net (IOException: closed).");
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1", "test-model",
                0.2, 100, Duration.ofSeconds(1));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        var response = service.completeStreaming(new AiCompletionRequest(recording(), "en", "prompt"),
                ignored -> { });

        assertEquals("{\"summaryMarkdown\":\"fallback\"}", response.text());
        assertEquals(1, transport.streamCalls);
        assertEquals(1, transport.postCalls);
        JsonNode fallbackBody = MAPPER.readTree(transport.body);
        assertEquals(false, fallbackBody.path("stream").asBoolean());
    }

    @Test
    void rejectsResponseWithoutMessageContent() {
        FakeTransport transport = new FakeTransport(200, "{\"choices\":[{\"message\":{}}]}");
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1", "test-model",
                0.2, 100, Duration.ofSeconds(1));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.complete(new AiCompletionRequest(recording(), "en", "prompt")));

        assertEquals("AI provider response did not contain message content.", exception.getMessage());
    }

    @Test
    void rejectsInvalidProviderJson() {
        FakeTransport transport = new FakeTransport(200, "not-json");
        var settings = new OpenAiCompatibleAiSettings("test-key", "https://example.test/v1", "test-model",
                0.2, 100, Duration.ofSeconds(1));
        var service = new OpenAiCompatibleCompletionService(() -> settings, transport);

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.complete(new AiCompletionRequest(recording(), "en", "prompt")));

        assertEquals("AI provider returned invalid JSON.", exception.getMessage());
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("rec-1", Path.of("recording.jfr"), "recording.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), 60_000, 1024);
    }

    private static final class FakeTransport implements OpenAiCompatibleHttpTransport {

        private final int statusCode;
        private final String responseBody;
        private String streamResponseBody;
        private URI uri;
        private Map<String, String> headers;
        private String body;
        private Duration timeout;
        private JmcFxException streamFailure;
        private int postCalls;
        private int streamCalls;

        private FakeTransport(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        @Override
        public HttpResponse post(URI uri, Map<String, String> headers, String body, Duration timeout) {
            postCalls++;
            this.uri = uri;
            this.headers = Map.copyOf(headers);
            this.body = body;
            this.timeout = timeout;
            return new HttpResponse(statusCode, responseBody);
        }

        @Override
        public HttpResponse postStream(URI uri, Map<String, String> headers, String body, Duration timeout,
                ContentDeltaListener listener) {
            streamCalls++;
            this.uri = uri;
            this.headers = Map.copyOf(headers);
            this.body = body;
            this.timeout = timeout;
            if (streamFailure != null) {
                throw streamFailure;
            }
            if (statusCode < 200 || statusCode >= 300) {
                return new HttpResponse(statusCode, responseBody);
            }
            streamResponseBody = streamResponseBody.strip();
            if (listener != null) {
                listener.onContentDelta(streamResponseBody);
            }
            return new HttpResponse(statusCode, streamResponseBody);
        }
    }
}
