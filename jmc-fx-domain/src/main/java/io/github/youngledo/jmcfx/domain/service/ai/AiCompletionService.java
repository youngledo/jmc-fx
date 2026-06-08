package io.github.youngledo.jmcfx.domain.service.ai;

import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;

public interface AiCompletionService {
    AiCompletionResponse complete(AiCompletionRequest request);
}
