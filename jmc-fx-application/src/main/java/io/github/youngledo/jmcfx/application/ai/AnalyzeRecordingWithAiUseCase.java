package io.github.youngledo.jmcfx.application.ai;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionStreamListener;
import io.github.youngledo.jmcfx.domain.service.ai.StreamingAiCompletionService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

public final class AnalyzeRecordingWithAiUseCase {

    private final AnalyzeRulesUseCase analyzeRules;
    private final AiCompletionService completionService;
    private final RecordingAiPromptBuilder promptBuilder;
    private final AiReportJsonParser reportParser;

    public AnalyzeRecordingWithAiUseCase(AnalyzeRulesUseCase analyzeRules, AiCompletionService completionService) {
        this(analyzeRules, completionService, new RecordingAiPromptBuilder(), new AiReportJsonParser());
    }

    AnalyzeRecordingWithAiUseCase(AnalyzeRulesUseCase analyzeRules, AiCompletionService completionService,
            RecordingAiPromptBuilder promptBuilder, AiReportJsonParser reportParser) {
        this.analyzeRules = Objects.requireNonNull(analyzeRules, "analyzeRules");
        this.completionService = Objects.requireNonNull(completionService, "completionService");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.reportParser = Objects.requireNonNull(reportParser, "reportParser");
    }

    public AiRecordingReport analyze(RecordingSummary recording, String languageTag) {
        return analyze(recording, languageTag, null);
    }

    public AiRecordingReport analyzeStreaming(RecordingSummary recording, String languageTag,
            AiCompletionStreamListener listener) {
        if (completionService instanceof StreamingAiCompletionService streamingCompletionService) {
            return analyze(recording, languageTag,
                    request -> streamingCompletionService.completeStreaming(request, listener));
        }
        return analyze(recording, languageTag);
    }

    private AiRecordingReport analyze(RecordingSummary recording, String languageTag,
            CompletionInvoker completionInvoker) {
        Objects.requireNonNull(recording, "recording");
        List<RuleResult> ruleResults = analyzeRules.analyze(recording);
        String prompt = promptBuilder.build(recording, ruleResults, languageTag);
        AiCompletionRequest request = new AiCompletionRequest(recording, languageTag, prompt);
        AiCompletionResponse response = completionInvoker == null
                ? completionService.complete(request)
                : completionInvoker.complete(request);
        try {
            return reportParser.parse(response.text());
        } catch (IllegalArgumentException e) {
            throw new JmcFxException("AI returned an invalid recording report.", e);
        }
    }

    @FunctionalInterface
    private interface CompletionInvoker {
        AiCompletionResponse complete(AiCompletionRequest request);
    }
}
