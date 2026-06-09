package io.github.youngledo.jmcfx.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.IntStream;
import java.util.List;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.DependencyGraphReport;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.G1GcReport;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataReport;
import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.domain.model.StackTreeNode;
import io.github.youngledo.jmcfx.domain.model.ThreadSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.domain.service.ExceptionService;
import io.github.youngledo.jmcfx.domain.service.FileIOService;
import io.github.youngledo.jmcfx.domain.service.G1GcService;
import io.github.youngledo.jmcfx.domain.service.HeapService;
import io.github.youngledo.jmcfx.domain.service.JfrMetadataService;
import io.github.youngledo.jmcfx.domain.service.LockService;
import io.github.youngledo.jmcfx.domain.service.ProfilingService;
import io.github.youngledo.jmcfx.domain.service.SocketIOService;
import io.github.youngledo.jmcfx.domain.service.ThreadService;
import io.github.youngledo.jmcfx.domain.service.TlabService;
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
    void parsesStructuredReportFromMarkdownResponseWithFinalJsonBlock() {
        RecordingCompletionService completionService = new RecordingCompletionService("""
                ## Initial read

                GC pressure is the main issue.

                ```jmcfx-report-json
                {
                  "summaryMarkdown": "Structured summary",
                  "findings": [],
                  "followUpQuestions": ["What should I inspect next?"],
                  "contextLimitations": []
                }
                ```
                """);
        var useCase = new AnalyzeRecordingWithAiUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                completionService);

        AiRecordingReport report = useCase.analyze(recording(), "en");

        assertEquals("Structured summary", report.summaryMarkdown());
        assertEquals("What should I inspect next?", report.followUpQuestions().getFirst());
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
    void acceptsCommonLocalizedSeverityValuesAsParserFallback() {
        var completionService = new RecordingCompletionService("""
                {
                  "summaryMarkdown": "报告",
                  "findings": [
                    {"title": "严重问题", "severity": "严重", "confidence": 0.9},
                    {"title": "警告问题", "severity": "警告", "confidence": 0.6},
                    {"title": "提示问题", "severity": "提示", "confidence": 0.3}
                  ],
                  "followUpQuestions": [],
                  "contextLimitations": []
                }
                """);
        var useCase = new AnalyzeRecordingWithAiUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                completionService);

        AiRecordingReport report = useCase.analyze(recording(), "zh-CN");

        assertEquals(AiSeverity.CRITICAL, report.findings().get(0).severity());
        assertEquals(AiSeverity.WARNING, report.findings().get(1).severity());
        assertEquals(AiSeverity.INFO, report.findings().get(2).severity());
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
    void expandsRecordingAiContextWithCappedDeterministicSummaries() {
        var useCase = new BuildRecordingAiContextUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                new FakeJfrMetadataService(35),
                new FakeG1GcService(),
                new FakeExceptionService(),
                new FakeProfilingService(),
                recording -> IntStream.rangeClosed(1, 24)
                        .mapToObj(index -> new ThreadSummary("thread-" + index, index, "", false, index,
                                index * 10L, List.of()))
                        .toList(),
                new FakeHeapService(),
                new FakeTlabService(),
                (recording, grouping) -> IntStream.rangeClosed(1, 21)
                        .mapToObj(index -> new LockHistogram("Lock" + index, index, index * 10L,
                                index, index, 0, index, index))
                        .toList(),
                new FakeFileIOService(),
                new FakeSocketIOService());

        RecordingAiContext context = useCase.build(recording());

        assertEquals(10, context.sections().size());
        assertEquals("JFR metadata", context.sections().getFirst().title());
        assertEquals(30, context.sections().getFirst().rows().size());
        assertEquals(10, context.sections().stream()
                .filter(RecordingAiContextSection::capped)
                .count());
        assertTrue(context.limitations().contains("JFR metadata were capped to top 30 of 35."));
        assertTrue(context.limitations().contains("Socket I/O were capped to top 20 of 21."));
    }

    @Test
    void sendsExpandedContextInAnalysisPrompt() {
        RecordingCompletionService completionService = new RecordingCompletionService("""
                {"summaryMarkdown":"Report","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                """);
        var contextBuilder = new BuildRecordingAiContextUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                new FakeJfrMetadataService(1), null, null, null, null, null, null, null, null, null);
        var useCase = new AnalyzeRecordingWithAiUseCase(contextBuilder, completionService);

        useCase.analyze(recording(), "en");

        assertTrue(completionService.lastRequest.prompt().contains("Stream a user-visible Markdown report first"));
        assertTrue(completionService.lastRequest.prompt().contains("```jmcfx-report-json"));
        assertTrue(completionService.lastRequest.prompt().contains("Additional deterministic context"));
        assertTrue(completionService.lastRequest.prompt().contains("JFR metadata"));
        assertTrue(completionService.lastRequest.prompt().contains("eventType id=jdk.Event1"));
    }

    @Test
    void instructsProviderToLocalizeAllUserVisibleReportText() {
        RecordingCompletionService completionService = new RecordingCompletionService("""
                {"summaryMarkdown":"报告","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                """);
        var useCase = new AnalyzeRecordingWithAiUseCase(new AnalyzeRulesUseCase(ignored -> List.of()),
                completionService);

        useCase.analyze(recording(), "zh-CN");

        assertTrue(completionService.lastRequest.prompt().contains("Response language: zh-CN"));
        assertTrue(completionService.lastRequest.prompt().contains("Every user-visible text field"));
        assertTrue(completionService.lastRequest.prompt().contains("Do not localize machine-readable fields"));
        assertTrue(completionService.lastRequest.prompt()
                .contains("severity must be exactly one of info, warning, or critical"));
        assertTrue(completionService.lastRequest.prompt()
                .contains("confidence must be a number between 0.0 and 1.0"));
        assertTrue(completionService.lastRequest.prompt().contains("contextLimitations"));
        assertTrue(completionService.lastRequest.prompt()
                .contains("Do not copy context limitation text verbatim when the response language is not English"));
    }

    @Test
    void capsExpandedContextPromptBeforeSendingProviderRequest() {
        RecordingCompletionService completionService = new RecordingCompletionService("""
                {"summaryMarkdown":"Report","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                """);
        String longRow = "x".repeat(2_000);
        var context = new RecordingAiContext(recording(), List.of(), List.of(
                new RecordingAiContextSection("large", "Large section",
                        IntStream.rangeClosed(1, 300)
                                .mapToObj(index -> "row-" + index + " " + longRow)
                                .toList(),
                        true, 300, 300)),
                List.of());
        var useCase = new AnalyzeRecordingWithAiUseCase(new BuildRecordingAiContextUseCase(
                new AnalyzeRulesUseCase(ignored -> List.of())), completionService);

        useCase.analyze(context, "en");

        assertTrue(completionService.lastRequest.prompt().length() <= 60_000);
        assertTrue(completionService.lastRequest.prompt().contains("Context budget limitation"));
        assertTrue(completionService.lastRequest.prompt().contains("[truncated]"));
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

    private static final class FakeJfrMetadataService implements JfrMetadataService {
        private final int eventTypeCount;

        private FakeJfrMetadataService(int eventTypeCount) {
            this.eventTypeCount = eventTypeCount;
        }

        @Override
        public JfrMetadataReport loadMetadata(RecordingSummary recording) {
            return new JfrMetadataReport(IntStream.rangeClosed(1, eventTypeCount)
                    .mapToObj(index -> new JfrMetadataEventType("jdk.Event" + index, "Event " + index,
                            List.of("JDK"), index, "", List.of()))
                    .toList());
        }
    }

    private static final class FakeG1GcService implements G1GcService {
        @Override
        public G1GcReport loadG1GcReport(RecordingSummary recording) {
            return new G1GcReport(0, 0, 12, 0, 0, 0, Instant.EPOCH, List.of(), List.of(),
                    IntStream.rangeClosed(1, 12)
                            .mapToObj(index -> new GcEvent(index, "G1", "Allocation Failure",
                                    index * 100L, index * 120L, Instant.EPOCH.plusSeconds(index)))
                            .toList());
        }
    }

    private static final class FakeExceptionService implements ExceptionService {
        @Override
        public List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping) {
            return IntStream.rangeClosed(1, 22)
                    .mapToObj(index -> new ExceptionSummary("key-" + index, "Exception" + index,
                            "message " + index, index, index))
                    .toList();
        }

        @Override
        public ChartDefinition loadTimeline(RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class FakeProfilingService implements ProfilingService {
        @Override
        public List<HotMethod> loadHotMethods(RecordingSummary recording) {
            return IntStream.rangeClosed(1, 32)
                    .mapToObj(index -> new HotMethod("Class.method" + index, "java", index, index))
                    .toList();
        }

        @Override
        public StackTreeNode loadFlameGraphTree(RecordingSummary recording, boolean invertedStacks) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public StackTreeNode loadFlameGraphTree(RecordingSummary recording, String method, boolean invertedStacks) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class FakeHeapService implements HeapService {
        @Override
        public List<HeapClassHistogram> loadHeapClassHistogram(RecordingSummary recording) {
            return IntStream.rangeClosed(1, 21)
                    .mapToObj(index -> new HeapClassHistogram("Class" + index, index, index * 1024L,
                            0, index))
                    .toList();
        }

        @Override
        public ChartDefinition loadHeapUsageTimeline(RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class FakeTlabService implements TlabService {
        @Override
        public List<TlabAllocation> loadTlabAllocations(RecordingSummary recording) {
            return IntStream.rangeClosed(1, 23)
                    .mapToObj(index -> new TlabAllocation("thread-" + index, index, 0, 0, 0,
                            index * 100L, 0))
                    .toList();
        }

        @Override
        public ChartDefinition loadTlabAllocationTimeline(RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class FakeFileIOService implements FileIOService {
        @Override
        public List<FileIOHistogram> loadFileIOHistogram(RecordingSummary recording) {
            return IntStream.rangeClosed(1, 21)
                    .mapToObj(index -> new FileIOHistogram("/tmp/file-" + index, index, 0,
                            index * 100L, 0, index, index, index))
                    .toList();
        }

        @Override
        public List<io.github.youngledo.jmcfx.domain.model.FileIOEvent> loadFileIOEvents(
                RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public ChartDefinition loadTimeline(RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class FakeSocketIOService implements SocketIOService {
        @Override
        public List<SocketIOHistogram> loadSocketIOHistogram(RecordingSummary recording, SocketIOGrouping grouping) {
            return IntStream.rangeClosed(1, 21)
                    .mapToObj(index -> new SocketIOHistogram("localhost:" + index, "localhost", index,
                            index, 0, index * 100L, 0, index, index, index))
                    .toList();
        }

        @Override
        public List<io.github.youngledo.jmcfx.domain.model.SocketIOEvent> loadSocketIOEvents(
                RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public ChartDefinition loadTimeline(RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }
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
