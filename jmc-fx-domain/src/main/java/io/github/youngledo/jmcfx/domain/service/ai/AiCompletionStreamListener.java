package io.github.youngledo.jmcfx.domain.service.ai;

@FunctionalInterface
public interface AiCompletionStreamListener {

    void onContentDelta(String contentDelta);
}
