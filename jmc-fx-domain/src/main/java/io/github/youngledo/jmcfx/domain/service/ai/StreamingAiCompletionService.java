package io.github.youngledo.jmcfx.domain.service.ai;

import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;

public interface StreamingAiCompletionService extends AiCompletionService {

    AiCompletionResponse completeStreaming(AiCompletionRequest request, AiCompletionStreamListener listener);
}
