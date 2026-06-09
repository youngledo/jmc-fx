package io.github.youngledo.jmcfx.application.ai;

import java.util.Objects;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionStreamListener;
import io.github.youngledo.jmcfx.domain.service.ai.StreamingAiCompletionService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

public final class AnalyzeRecordingWithAiUseCase {

    private final BuildRecordingAiContextUseCase buildContextUseCase;
    private final AiCompletionService completionService;
    private final RecordingAiPromptBuilder promptBuilder;
    private final AiReportJsonParser reportParser;

    public AnalyzeRecordingWithAiUseCase(AnalyzeRulesUseCase analyzeRules, AiCompletionService completionService) {
        this(new BuildRecordingAiContextUseCase(analyzeRules), completionService);
    }

    public AnalyzeRecordingWithAiUseCase(BuildRecordingAiContextUseCase buildContextUseCase,
            AiCompletionService completionService) {
        this(buildContextUseCase, completionService, new RecordingAiPromptBuilder(), new AiReportJsonParser());
    }

    AnalyzeRecordingWithAiUseCase(BuildRecordingAiContextUseCase buildContextUseCase,
            AiCompletionService completionService,
            RecordingAiPromptBuilder promptBuilder, AiReportJsonParser reportParser) {
        this.buildContextUseCase = Objects.requireNonNull(buildContextUseCase, "buildContextUseCase");
        this.completionService = Objects.requireNonNull(completionService, "completionService");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.reportParser = Objects.requireNonNull(reportParser, "reportParser");
    }

    public AiRecordingReport analyze(RecordingSummary recording, String languageTag) {
        Objects.requireNonNull(recording, "recording");
        return analyze(buildContextUseCase.build(recording), languageTag);
    }

    public AiRecordingReport analyze(RecordingAiContext context, String languageTag) {
        return analyze(context, languageTag, null);
    }

    public AiRecordingReport analyzeStreaming(RecordingSummary recording, String languageTag,
            AiCompletionStreamListener listener) {
        Objects.requireNonNull(recording, "recording");
        return analyzeStreaming(buildContextUseCase.build(recording), languageTag, listener);
    }

    public AiRecordingReport analyzeStreaming(RecordingAiContext context, String languageTag,
            AiCompletionStreamListener listener) {
        if (completionService instanceof StreamingAiCompletionService streamingCompletionService) {
            return analyze(context, languageTag,
                    request -> streamingCompletionService.completeStreaming(request, listener));
        }
        return analyze(context, languageTag);
    }

    private AiRecordingReport analyze(RecordingAiContext context, String languageTag,
            CompletionInvoker completionInvoker) {
        Objects.requireNonNull(context, "context");
        String prompt = promptBuilder.build(context, languageTag);
        AiCompletionRequest request = new AiCompletionRequest(context.recording(), languageTag, prompt);
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
