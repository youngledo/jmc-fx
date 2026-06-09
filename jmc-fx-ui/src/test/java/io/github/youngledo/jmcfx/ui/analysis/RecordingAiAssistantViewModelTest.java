package io.github.youngledo.jmcfx.ui.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.ai.AnalyzeRecordingWithAiUseCase;
import io.github.youngledo.jmcfx.application.ai.AskRecordingAssistantUseCase;
import io.github.youngledo.jmcfx.application.ai.AiSettingsUseCase;
import io.github.youngledo.jmcfx.application.ai.BuildRecordingAiContextUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.RuleAnalysisService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionStreamListener;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;
import io.github.youngledo.jmcfx.domain.service.ai.StreamingAiCompletionService;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import org.junit.jupiter.api.Test;

class RecordingAiAssistantViewModelTest {

    @Test
    void buildsPreviewBeforeSendingAiRequest() {
        RecordingCompletionService completionService = new RecordingCompletionService();
        RecordingAiAssistantViewModel viewModel = viewModel(completionService);

        viewModel.preparePreview(recording());

        assertTrue(viewModel.previewReadyProperty().get());
        assertTrue(viewModel.contextPreviewProperty().get().contains("Rule results sent: 1"));
        assertEquals(0, completionService.calls);
    }

    @Test
    void analyzesAndStoresRecordingScopedReport() {
        RecordingCompletionService completionService = new RecordingCompletionService();
        completionService.response = """
                {
                  "summaryMarkdown": "GC pressure is elevated.",
                  "findings": [
                    {
                      "title": "Long GC pause",
                      "severity": "warning",
                      "confidence": 0.8,
                      "relatedPageId": "gcDetails",
                      "recommendedNextStepMarkdown": "Inspect GC pauses.",
                      "limitationsMarkdown": "",
                      "evidence": [{"label": "Max pause", "value": "250 ms", "source": "rules"}]
                    }
                  ],
                  "followUpQuestions": ["Which GC page should I inspect?"],
                  "contextLimitations": ["Rule results only."]
                }
                """;
        RecordingAiAssistantViewModel viewModel = viewModel(completionService);

        viewModel.preparePreview(recording());
        viewModel.analyze("en");

        assertTrue(viewModel.reportReadyProperty().get());
        assertFalse(viewModel.previewReadyProperty().get());
        assertEquals("GC pressure is elevated.", viewModel.reportSummaryProperty().get());
        assertTrue(viewModel.reportTextProperty().get().contains("Summary"));
        assertTrue(viewModel.reportTextProperty().get().contains("Long GC pause"));
        assertTrue(viewModel.reportTextProperty().get().contains("Max pause: 250 ms"));
        assertEquals(1, viewModel.findingsProperty().size());
        assertEquals("Long GC pause", viewModel.findingsProperty().getFirst().title());
        assertEquals("Rule results only.", viewModel.contextLimitationsProperty().get());
    }

    @Test
    void usesStreamingProviderWithoutPublishingRawJsonAsReportText() {
        StreamingRecordingCompletionService completionService = new StreamingRecordingCompletionService();
        completionService.response = """
                {"summaryMarkdown":"Streamed report","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                """;
        RecordingAiAssistantViewModel viewModel = viewModel(completionService);

        viewModel.preparePreview(recording());
        viewModel.analyze("en");

        assertTrue(completionService.sawIntermediateReportText);
        assertTrue(viewModel.reportReadyProperty().get());
        assertTrue(viewModel.reportTextProperty().get().contains("Summary"));
        assertTrue(viewModel.reportTextProperty().get().contains("Streamed report"));
        assertFalse(viewModel.reportTextProperty().get().contains("summaryMarkdown"));
    }

    @Test
    void reportsUnavailableWhenCompletionUseCaseIsMissing() {
        RecordingAiAssistantViewModel viewModel = new RecordingAiAssistantViewModel(
                new BuildRecordingAiContextUseCase(new AnalyzeRulesUseCase(ruleService())),
                null,
                null,
                null,
                Runnable::run,
                formatter(Locale.ENGLISH));

        viewModel.preparePreview(recording());

        assertFalse(viewModel.availableProperty().get());
        assertTrue(viewModel.errorProperty().get());
        assertTrue(viewModel.errorMessageProperty().get().contains("OPENAI_API_KEY"));
    }

    @Test
    void doesNotBuildPreviewWhenProviderSettingsAreIncomplete() {
        RecordingCompletionService completionService = new RecordingCompletionService();
        RecordingAiAssistantViewModel viewModel = viewModel(completionService,
                new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                        new AiSettings(true, "https://api.openai.com/v1", "", 0.2, 4_096, false))),
                        Map.of()));

        viewModel.preparePreview(recording());

        assertFalse(viewModel.availableProperty().get());
        assertTrue(viewModel.errorProperty().get());
        assertFalse(viewModel.previewReadyProperty().get());
        assertEquals("", viewModel.contextPreviewProperty().get());
        assertEquals(0, completionService.calls);
    }

    private static RecordingAiAssistantViewModel viewModel(AiCompletionService completionService) {
        return viewModel(completionService,
                new AiSettingsUseCase(new FakeAiSettingsRepository(Optional.of(
                        new AiSettings(true, "https://api.openai.com/v1", "model", 0.2, 4_096, false))),
                        Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key")));
    }

    private static RecordingAiAssistantViewModel viewModel(AiCompletionService completionService,
            AiSettingsUseCase aiSettingsUseCase) {
        AnalyzeRulesUseCase rules = new AnalyzeRulesUseCase(ruleService());
        return new RecordingAiAssistantViewModel(
                new BuildRecordingAiContextUseCase(rules),
                new AnalyzeRecordingWithAiUseCase(rules, completionService),
                new AskRecordingAssistantUseCase(completionService),
                aiSettingsUseCase,
                Runnable::run,
                formatter(Locale.ENGLISH));
    }

    private static RecordingAiReportFormatter formatter(Locale locale) {
        I18n i18n = new I18n(locale);
        i18n.setLanguageMode(Locale.SIMPLIFIED_CHINESE.equals(locale)
                ? LanguageMode.CHINESE_SIMPLIFIED : LanguageMode.ENGLISH);
        return new RecordingAiReportFormatter(i18n);
    }

    private static RuleAnalysisService ruleService() {
        return recording -> List.of(new RuleResult("gc.pause", "Long GC pause",
                Severity.WARNING, 80, "GC", "A long GC pause occurred.", "Details",
                "Pause evidence",
                "Inspect GC details.", "gcDetails"));
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("recording", Path.of("recording.jfr"), "recording.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static final class RecordingCompletionService implements AiCompletionService {
        private int calls;
        private String response = """
                {"summaryMarkdown": "Summary", "findings": [], "followUpQuestions": [], "contextLimitations": []}
                """;

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            calls++;
            return new AiCompletionResponse(response);
        }
    }

    private static final class StreamingRecordingCompletionService
            implements StreamingAiCompletionService {
        private int calls;
        private String response = """
                {"summaryMarkdown": "Summary", "findings": [], "followUpQuestions": [], "contextLimitations": []}
                """;
        private boolean sawIntermediateReportText;

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            calls++;
            return new AiCompletionResponse(response);
        }

        @Override
        public AiCompletionResponse completeStreaming(AiCompletionRequest request,
                AiCompletionStreamListener listener) {
            calls++;
            String stripped = response.strip();
            int midpoint = stripped.length() / 2;
            listener.onContentDelta(stripped.substring(0, midpoint));
            sawIntermediateReportText = true;
            listener.onContentDelta(stripped.substring(midpoint));
            return new AiCompletionResponse(stripped);
        }
    }

    private static final class FakeAiSettingsRepository implements AiSettingsRepository {
        private final Optional<AiSettings> settings;

        private FakeAiSettingsRepository(Optional<AiSettings> settings) {
            this.settings = settings;
        }

        @Override
        public Optional<AiSettings> load() {
            return settings;
        }

        @Override
        public void save(AiSettings settings) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }
}
