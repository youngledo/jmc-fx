package io.github.youngledo.jmcfx.adapter.ai;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

interface OpenAiCompatibleHttpTransport {

    HttpResponse post(URI uri, Map<String, String> headers, String body, Duration timeout);

    record HttpResponse(int statusCode, String body) {
        public HttpResponse {
            body = body == null ? "" : body;
        }
    }
}
