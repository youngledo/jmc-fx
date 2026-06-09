package io.github.youngledo.jmcfx.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.IntStream;
import java.util.List;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionStreamListener;
import io.github.youngledo.jmcfx.domain.service.ai.StreamingAiCompletionService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import org.junit.jupiter.api.Test;

class AnalyzeRecordingWithAiUseCaseTest {

    @Test
    void analyzesRecordingWithStructuredAiReport() {
        RecordingSummary recording = recording();
        var ruleUseCase = new AnalyzeRulesUseCase(ignored -> List.of(new RuleResult(
                "rule-1", "Long GC pause", Severity.WARNING, 75, "gc",
                "Long pause detected", "The longest pause exceeded the threshold.",
                "Longest pause: 2.8 s", "Inspect GC details.", "g1Gc")));
        var completionService = new RecordingCompletionService("""
                {
                  "summaryMarkdown": "The recording shows **GC pressure**.",
                  "findings": [
                    {
                      "title": "Long GC pause",
                      "severity": "warning",
                      "confidence": 0.82,
                      "relatedPageId": "g1Gc",
                      "recommendedNextStepMarkdown": "Open `GC` details.",
                      "limitationsMarkdown": "Only top-N rule results were sent.",
                      "evidence": [
                        {
                          "label": "Rule",
                          "value": "Long pause detected",
                          "source": "rules",
                          "relatedPageId": "analysis",
                          "relatedEntityId": "rule-1"
                        }
                      ]
                    }
                  ],
                  "followUpQuestions": [
                    "Why is GC considered the main issue?"
                  ],
                  "contextLimitations": [
                    "Context is capped to top-N summaries."
                  ]
                }
                """);
        var useCase = new AnalyzeRecordingWithAiUseCase(ruleUseCase, completionService);

        AiRecordingReport report = useCase.analyze(recording, "en");

        assertEquals("The recording shows **GC pressure**.", report.summaryMarkdown());
        assertEquals("Why is GC considered the main issue?", report.followUpQuestions().getFirst());
        assertEquals("Context is capped to top-N summaries.", report.contextLimitations().getFirst());
        assertEquals(1, report.findings().size());
        var finding = report.findings().getFirst();
        assertEquals("Long GC pause", finding.title());
        assertEquals(AiSeverity.WARNING, finding.severity());
        assertEquals(0.82, finding.confidence());
        assertEquals("g1Gc", finding.relatedPageId());
        assertEquals("Open `GC` details.", finding.recommendedNextStepMarkdown());
        assertEquals("Only top-N rule results were sent.", finding.limitationsMarkdown());
        assertEquals(1, finding.evidence().size());
        var evidence = finding.evidence().getFirst();
        assertEquals("Rule", evidence.label());
        assertEquals("Long pause detected", evidence.value());
        assertEquals("rules", evidence.source());
        assertEquals("analysis", evidence.relatedPageId());
        assertEquals("rule-1", evidence.relatedEntityId());
        assertEquals(recording, completionService.lastRequest.recording());
    }

    @Test
    void capsAndNormalizesAiReportOutput() {
        var completionService = new RecordingCompletionService("""
                {
                  "summaryMarkdown": "Report",
                  "findings": [
                    %s
                  ],
                  "followUpQuestions": ["q1", "q2", "q3", "q4", "q5", "q6", "q7"],
                  "contextLimitations": []
                }
                """.formatted(IntStream.rangeClosed(1, 9)
                .mapToObj(index -> """
                        {
                          "title": "Finding %d",
                          "severity": "%s",
                          "confidence": %s,
                          "evidence": [
                            {"label": "e1", "value": "v1", "source": "rules"},
                            {"label": "e2", "value": "v2", "source": "rules"},
                            {"label": "e3", "value": "v3", "source": "rules"},
                            {"label": "e4", "value": "v4", "source": "rules"},
                            {"label": "e5", "value": "v5", "source": "rules"},
                            {"label": "e6", "value": "v6", "source": "rules"}
                          ]
                        }
                        """.formatted(index, index == 1 ? "unexpected" : "warning", index == 1 ? "2.5" : "0.5"))
                .collect(java.util.stream.Collectors.joining(","))));
        var useCase = new AnalyzeRecordingWithAiUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                completionService);

        AiRecordingReport report = useCase.analyze(recording(), "en");

        assertEquals(8, report.findings().size());
        assertEquals(AiSeverity.UNKNOWN, report.findings().getFirst().severity());
        assertEquals(1.0, report.findings().getFirst().confidence());
        assertEquals(5, report.findings().getFirst().evidence().size());
        assertEquals(6, report.followUpQuestions().size());
    }

    @Test
    void rejectsInvalidAiReportJsonWithApplicationException() {
        var useCase = new AnalyzeRecordingWithAiUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                new RecordingCompletionService("not-json"));

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> useCase.analyze(recording(), "en"));

        assertEquals("AI returned an invalid recording report.", exception.getMessage());
    }

    @Test
    void streamsAiReportContentBeforeParsingFinalJson() {
        StreamingRecordingCompletionService completionService = new StreamingRecordingCompletionService("""
                {"summaryMarkdown":"Streamed report","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                """);
        var useCase = new AnalyzeRecordingWithAiUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                completionService);
        StringBuilder streamedText = new StringBuilder();

        AiRecordingReport report = useCase.analyzeStreaming(recording(), "en", streamedText::append);

        assertEquals("Streamed report", report.summaryMarkdown());
        assertEquals("""
                {"summaryMarkdown":"Streamed report","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                """.strip(), streamedText.toString());
    }

    @Test
    void buildsPreviewableRecordingAiContextFromRules() {
        List<RuleResult> rules = IntStream.rangeClosed(1, 25)
                .mapToObj(index -> new RuleResult(
                        "rule-" + index,
                        "Rule " + index,
                        Severity.WARNING,
                        index,
                        "topic",
                        "Summary " + index,
                        "Explanation " + index,
                        "Evidence " + index,
                        "Recommendation " + index,
                        "analysis"))
                .toList();
        var useCase = new BuildRecordingAiContextUseCase(new AnalyzeRulesUseCase(ignored -> rules));

        RecordingAiContext context = useCase.build(recording());

        assertEquals(recording(), context.recording());
        assertEquals(20, context.ruleResults().size());
        assertEquals("rule-25", context.ruleResults().getFirst().id());
        assertEquals("Rule results were capped to top 20 by score.", context.limitations().getFirst());
    }

    @Test
    void answersFollowUpQuestionFromExistingReportAndContext() {
        RecordingCompletionService completionService = new RecordingCompletionService("""
                {
                  "answerMarkdown": "GC is the likely issue because the report cites `rule-1`.",
                  "followUpQuestions": ["Which page should I inspect next?"]
                }
                """);
        var report = new AiRecordingReport("Summary", List.of(new AiFinding(
                "Long GC pause", AiSeverity.WARNING, 0.8, "g1Gc", "Inspect GC.", "",
                List.of(new AiEvidence(
                        "Rule", "Long pause detected", "rules", "analysis", "rule-1")))),
                List.of(), List.of());
        var context = new RecordingAiContext(recording(), List.of(), List.of("Context is capped."));
        var useCase = new AskRecordingAssistantUseCase(completionService);

        var answer = useCase.ask(context, report, "Why GC?", "en");

        assertEquals("GC is the likely issue because the report cites `rule-1`.", answer.answerMarkdown());
        assertEquals("Which page should I inspect next?", answer.followUpQuestions().getFirst());
        assertEquals(recording(), completionService.lastRequest.recording());
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("rec-1", Path.of("recording.jfr"), "recording.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), 60_000, 1024);
    }

    private static final class RecordingCompletionService implements AiCompletionService {
        private final String responseText;
        private AiCompletionRequest lastRequest;

        RecordingCompletionService(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            lastRequest = request;
            return new AiCompletionResponse(responseText);
        }
    }

    private static final class StreamingRecordingCompletionService implements StreamingAiCompletionService {
        private final String responseText;

        StreamingRecordingCompletionService(String responseText) {
            this.responseText = responseText.strip();
        }

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            return new AiCompletionResponse(responseText);
        }

        @Override
        public AiCompletionResponse completeStreaming(AiCompletionRequest request,
                AiCompletionStreamListener listener) {
            int midpoint = responseText.length() / 2;
            listener.onContentDelta(responseText.substring(0, midpoint));
            listener.onContentDelta(responseText.substring(midpoint));
            return new AiCompletionResponse(responseText);
        }
    }
}
