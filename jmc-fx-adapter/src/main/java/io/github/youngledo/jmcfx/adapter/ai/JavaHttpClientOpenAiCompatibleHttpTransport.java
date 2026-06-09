package io.github.youngledo.jmcfx.adapter.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

final class JavaHttpClientOpenAiCompatibleHttpTransport implements OpenAiCompatibleHttpTransport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    JavaHttpClientOpenAiCompatibleHttpTransport() {
        this(HttpClient.newHttpClient());
    }

    JavaHttpClientOpenAiCompatibleHttpTransport(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public HttpResponse post(URI uri, Map<String, String> headers, String body, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        try {
            var response = httpClient.send(builder.build(), BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new JmcFxException(callFailureMessage(uri, exception), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JmcFxException("AI provider request was interrupted.", exception);
        }
    }

    @Override
    public HttpResponse postStream(URI uri, Map<String, String> headers, String body, Duration timeout,
            ContentDeltaListener listener) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        try {
            var response = httpClient.send(builder.build(), BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new HttpResponse(response.statusCode(), response.body().collect(java.util.stream.Collectors.joining("\n")));
            }
            StringBuilder content = new StringBuilder();
            try (var lines = response.body()) {
                lines.forEach(line -> appendSseLine(line, content, listener));
            }
            return new HttpResponse(response.statusCode(), content.toString());
        } catch (UncheckedIOException exception) {
            throw new JmcFxException(callFailureMessage(uri, exception.getCause()), exception.getCause());
        } catch (IOException exception) {
            throw new JmcFxException(callFailureMessage(uri, exception), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JmcFxException("AI provider request was interrupted.", exception);
        }
    }

    private static void appendSseLine(String line, StringBuilder content, ContentDeltaListener listener) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring("data:".length()).strip();
        if (data.isBlank() || "[DONE]".equals(data)) {
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(data);
            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
            if (delta.isTextual()) {
                String text = delta.asText();
                content.append(text);
                if (listener != null) {
                    listener.onContentDelta(text);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String callFailureMessage(URI uri, IOException exception) {
        String cause = exception.getClass().getSimpleName();
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            return "Unable to call AI provider at " + uri.getHost() + " (" + cause + ").";
        }
        return "Unable to call AI provider at " + uri.getHost() + " (" + cause + ": " + detail + ").";
    }
}
