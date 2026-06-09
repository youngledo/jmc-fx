package io.github.youngledo.jmcfx.adapter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import org.junit.jupiter.api.Test;

class JavaHttpClientOpenAiCompatibleHttpTransportTest {

    @Test
    void includesProviderHostAndIoCauseWhenHttpCallFails() {
        var transport = new JavaHttpClientOpenAiCompatibleHttpTransport(
                new FailingHttpClient(new IOException("connection refused")));

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> transport.post(URI.create("https://airoute.mycyjg.net/v1/chat/completions"),
                        Map.of(), "{}", Duration.ofSeconds(1)));

        assertEquals("Unable to call AI provider at airoute.mycyjg.net (IOException: connection refused).",
                exception.getMessage());
    }

    @Test
    void readsOpenAiCompatibleSseContentDeltas() {
        var transport = new JavaHttpClientOpenAiCompatibleHttpTransport(
                new StreamingHttpClient(Stream.of(
                        "data: {\"choices\":[{\"delta\":{\"content\":\"{\\\"summaryMarkdown\\\":\"}}]}",
                        "",
                        "data: {\"choices\":[{\"delta\":{\"content\":\"\\\"ok\\\"}\"}}]}",
                        "data: [DONE]")));
        StringBuilder streamedText = new StringBuilder();

        OpenAiCompatibleHttpTransport.HttpResponse response = transport.postStream(
                URI.create("https://airoute.mycyjg.net/v1/chat/completions"),
                Map.of(), "{}", Duration.ofSeconds(1), streamedText::append);

        assertEquals(200, response.statusCode());
        assertEquals("{\"summaryMarkdown\":\"ok\"}", response.body());
        assertEquals("{\"summaryMarkdown\":\"ok\"}", streamedText.toString());
    }

    private static final class FailingHttpClient extends HttpClient {

        private final IOException failure;

        private FailingHttpClient(IOException failure) {
            this.failure = failure;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> java.net.http.HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
                throws IOException {
            throw failure;
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(HttpRequest request,
                BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(HttpRequest request,
                BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class StreamingHttpClient extends BaseHttpClient {

        private final Stream<String> lines;

        private StreamingHttpClient(Stream<String> lines) {
            this.lines = lines;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> java.net.http.HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
            return (java.net.http.HttpResponse<T>) new FakeHttpResponse<>(200, lines);
        }
    }

    private abstract static class BaseHttpClient extends HttpClient {

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(HttpRequest request,
                BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(HttpRequest request,
                BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private record FakeHttpResponse<T>(int statusCode, T body) implements java.net.http.HttpResponse<T> {

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<java.net.http.HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public URI uri() {
            return URI.create("https://airoute.mycyjg.net/v1/chat/completions");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
