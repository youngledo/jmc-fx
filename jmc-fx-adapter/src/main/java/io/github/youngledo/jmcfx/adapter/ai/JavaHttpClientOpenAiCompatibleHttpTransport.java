package io.github.youngledo.jmcfx.adapter.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.service.JmcFxException;

final class JavaHttpClientOpenAiCompatibleHttpTransport implements OpenAiCompatibleHttpTransport {

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
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        try {
            var response = httpClient.send(builder.build(), BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new JmcFxException("Unable to call AI provider.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JmcFxException("AI provider request was interrupted.", exception);
        }
    }
}
