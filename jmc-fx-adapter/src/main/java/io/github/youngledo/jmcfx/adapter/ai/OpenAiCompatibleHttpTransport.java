package io.github.youngledo.jmcfx.adapter.ai;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

interface OpenAiCompatibleHttpTransport {

    HttpResponse post(URI uri, Map<String, String> headers, String body, Duration timeout);

    default HttpResponse postStream(URI uri, Map<String, String> headers, String body, Duration timeout,
            ContentDeltaListener listener) {
        return post(uri, headers, body, timeout);
    }

    @FunctionalInterface
    interface ContentDeltaListener {
        void onContentDelta(String contentDelta);
    }

    record HttpResponse(int statusCode, String body) {
        public HttpResponse {
            body = body == null ? "" : body;
        }
    }
}
