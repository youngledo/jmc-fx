package io.github.youngledo.jmcfx.ui.analysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.ai.AnalyzeRecordingWithAiUseCase;
import io.github.youngledo.jmcfx.application.ai.AskRecordingAssistantUseCase;
import io.github.youngledo.jmcfx.application.ai.AiSettingsUseCase;
import io.github.youngledo.jmcfx.application.ai.BuildRecordingAiContextUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionStreamListener;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;
import io.github.youngledo.jmcfx.domain.service.ai.StreamingAiCompletionService;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

class AnalysisPageControllerTest {

    @org.junit.jupiter.api.BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start in time.");
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void hidesAiAnalysisControlsWhenProviderIsUnavailable() {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        controller.configure();

        controller.bindAi(unavailableAiViewModel());

        assertFalse(view.aiAnalyzeButton().isVisible());
        assertFalse(view.aiReportView().node().isVisible());
    }

    @Test
    void refreshShowsAiAnalysisControlsAfterSettingsBecomeAvailable() {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        MutableAiSettingsRepository repository = new MutableAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "", 0.2, 4_096, false)));
        Map<String, String> environment = Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key");
        controller.configure();
        controller.bindAi(aiViewModel(repository, environment));

        assertFalse(view.aiAnalyzeButton().isVisible());

        repository.settings = Optional.of(new AiSettings(
                true, "https://api.openai.com/v1", "gpt-test", 0.2, 4_096, false));
        controller.refreshAiAvailability();

        assertTrue(view.aiAnalyzeButton().isVisible());
        assertTrue(view.aiReportView().node().isVisible());
    }

    @Test
    void rerunsAiAnalysisWithPrimaryActionAfterFailure() {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        CountingCompletionService completionService = new CountingCompletionService();
        RecordingAiAssistantViewModel viewModel = aiViewModel(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "gpt-test", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key"),
                completionService);
        controller.configure();
        controller.bindAi(viewModel);

        assertTrue(view.aiAnalyzeButton().isVisible());

        viewModel.setRecording(recording());
        view.aiAnalyzeButton().fire();

        assertEquals(1, completionService.calls);
        assertTrue(view.aiAnalyzeButton().isVisible());

        view.aiAnalyzeButton().fire();

        assertEquals(2, completionService.calls);
    }

    @Test
    void showsProgressWhileAiAnalysisIsWaitingForProviderResponse() throws Exception {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        BlockingCompletionService completionService = new BlockingCompletionService();
        RecordingAiAssistantViewModel viewModel = aiViewModel(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "gpt-test", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key"),
                completionService,
                command -> Thread.ofVirtual().start(command));
        controller.configure();
        controller.bindAi(viewModel);
        viewModel.setRecording(recording());

        view.aiAnalyzeButton().fire();

        assertTrue(completionService.started.await(5, TimeUnit.SECONDS));
        waitUntil(() -> hasStyleClass(view.aiReportView().node(), "ai-report-loading"));
        assertTrue(hasStyleClass(view.aiReportView().node(), "ai-report-loading"));
        assertTrue(view.aiAnalyzeButton().isVisible());
        assertTrue(view.aiAnalyzeButton().isDisabled());
        assertTrue(viewModel.analyzingProperty().get());
        assertTrue(viewModel.contextPreviewProperty().get().contains("Rule results sent: 0"));

        completionService.response.set(new AiCompletionResponse(
                "{\"summaryMarkdown\":\"ok\",\"findings\":[],\"followUpQuestions\":[],\"contextLimitations\":[]}"));
        completionService.release.countDown();

        waitUntil(() -> viewModel.reportReadyProperty().get());
        assertFalse(hasStyleClass(view.aiReportView().node(), "ai-report-loading"));
    }

    @Test
    void showsStreamingProgressWhileAiReportIsGenerating() throws Exception {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        BlockingStreamingCompletionService completionService = new BlockingStreamingCompletionService();
        RecordingAiAssistantViewModel viewModel = aiViewModel(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "gpt-test", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key"),
                completionService,
                command -> Thread.ofVirtual().start(command));
        controller.configure();
        controller.bindAi(viewModel);
        viewModel.setRecording(recording());

        view.aiAnalyzeButton().fire();

        assertTrue(completionService.streamed.await(5, TimeUnit.SECONDS));
        waitUntil(() -> reportText(view.aiReportView().node()).contains("Receiving AI response"));
        assertTrue(reportText(view.aiReportView().node()).contains("Receiving AI response"));
        assertFalse(reportText(view.aiReportView().node()).contains("Received"));
        assertFalse(reportText(view.aiReportView().node()).contains("characters"));
        assertFalse(reportText(view.aiReportView().node()).contains("Early AI read"));
        assertFalse(reportText(view.aiReportView().node()).contains("GC pressure is likely elevated"));
        assertFalse(reportText(view.aiReportView().node()).contains("summaryMarkdown"));
        assertTrue(viewModel.analyzingProperty().get());

        completionService.release.countDown();

        waitUntil(() -> reportText(view.aiReportView().node()).contains("completed"));
        assertTrue(viewModel.reportReadyProperty().get());
        assertTrue(reportText(view.aiReportView().node()).contains("completed"));
        assertTrue(reportText(view.aiReportView().node()).contains("Processed in"));
        assertTrue(view.aiAnalyzeButton().isDisabled());
    }

    @Test
    void clearsAiReportLoadingStateAfterProviderFailure() throws Exception {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        BlockingFailureCompletionService completionService = new BlockingFailureCompletionService();
        RecordingAiAssistantViewModel viewModel = aiViewModel(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "gpt-test", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key"),
                completionService,
                command -> Thread.ofVirtual().start(command));
        controller.configure();
        controller.bindAi(viewModel);
        viewModel.setRecording(recording());

        view.aiAnalyzeButton().fire();

        assertTrue(completionService.started.await(5, TimeUnit.SECONDS));
        waitUntil(() -> hasStyleClass(view.aiReportView().node(), "ai-report-loading"));
        completionService.release.countDown();

        waitUntil(() -> viewModel.errorProperty().get());
        assertFalse(hasStyleClass(view.aiReportView().node(), "ai-report-loading"));
        assertTrue(hasStyleClass(view.aiReportView().node(), "ai-report-error"));
    }

    @Test
    void showsAiProviderFailureDetailsOnlyInReportArea() throws Exception {
        AnalysisPaneView paneView = new AnalysisPaneView(new VBox());
        AnalysisPageView view = paneView.view();
        AnalysisPageController controller = new AnalysisPageController(view, new I18n(Locale.ENGLISH), section -> { });
        BlockingFailureCompletionService completionService = new BlockingFailureCompletionService();
        RecordingAiAssistantViewModel viewModel = aiViewModel(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "gpt-test", 0.2, 4_096, false))),
                Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key"),
                completionService,
                command -> Thread.ofVirtual().start(command));
        controller.configure();
        controller.bindAi(viewModel);
        viewModel.setRecording(recording());

        view.aiAnalyzeButton().fire();

        assertTrue(completionService.started.await(5, TimeUnit.SECONDS));
        completionService.release.countDown();
        waitUntil(() -> viewModel.errorProperty().get());
        waitUntil(() -> reportText(view.aiReportView().node()).contains("AI analysis failed: provider failed"));

        assertTrue(reportText(view.aiReportView().node()).contains("AI analysis failed: provider failed"));
    }

    private static RecordingAiAssistantViewModel unavailableAiViewModel() {
        return aiViewModel(new FakeAiSettingsRepository(Optional.of(
                new AiSettings(true, "https://api.openai.com/v1", "", 0.2, 4_096, false))), Map.of());
    }

    private static RecordingAiAssistantViewModel aiViewModel(AiSettingsRepository repository,
            Map<String, String> environment) {
        return aiViewModel(repository, environment, new FakeCompletionService());
    }

    private static RecordingAiAssistantViewModel aiViewModel(AiSettingsRepository repository,
            Map<String, String> environment, AiCompletionService completionService) {
        return aiViewModel(repository, environment, completionService, Runnable::run);
    }

    private static RecordingAiAssistantViewModel aiViewModel(AiSettingsRepository repository,
            Map<String, String> environment, AiCompletionService completionService,
            RecordingAiAssistantExecutor executor) {
        AnalyzeRulesUseCase rules = AnalyzeRulesUseCase.empty();
        return new RecordingAiAssistantViewModel(
                new BuildRecordingAiContextUseCase(rules),
                new AnalyzeRecordingWithAiUseCase(rules, completionService),
                new AskRecordingAssistantUseCase(completionService),
                new AiSettingsUseCase(repository, environment),
                executor,
                formatter(Locale.ENGLISH));
    }

    private static RecordingAiReportFormatter formatter(Locale locale) {
        I18n i18n = new I18n(locale);
        i18n.setLanguageMode(Locale.SIMPLIFIED_CHINESE.equals(locale)
                ? LanguageMode.CHINESE_SIMPLIFIED : LanguageMode.ENGLISH);
        return new RecordingAiReportFormatter(i18n);
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("recording", Path.of("recording.jfr"), "recording.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static final class FakeCompletionService implements AiCompletionService {
        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class CountingCompletionService implements AiCompletionService {
        private int calls;

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            calls++;
            throw new UnsupportedOperationException("expected failure");
        }
    }

    private static final class BlockingCompletionService implements AiCompletionService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicReference<AiCompletionResponse> response = new AtomicReference<>();

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", exception);
            }
            return response.get();
        }
    }

    private static final class BlockingFailureCompletionService implements AiCompletionService {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", exception);
            }
            throw new IllegalStateException("provider failed");
        }
    }

    private static final class BlockingStreamingCompletionService implements StreamingAiCompletionService {
        private final CountDownLatch streamed = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public AiCompletionResponse completeStreaming(AiCompletionRequest request,
                AiCompletionStreamListener listener) {
            listener.onContentDelta("## Early AI read\n\nGC pressure is likely elevated.");
            streamed.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", exception);
            }
            String jsonBlock = """

                    ```jmcfx-report-json
                    {"summaryMarkdown":"completed","findings":[],"followUpQuestions":[],"contextLimitations":[]}
                    ```
                    """;
            listener.onContentDelta(jsonBlock);
            return new AiCompletionResponse("## Early AI read\n\nGC pressure is likely elevated." + jsonBlock);
        }
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean());
    }

    private static boolean hasStyleClass(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return true;
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            return hasStyleClass(scrollPane.getContent(), styleClass);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (hasStyleClass(child, styleClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String reportText(Node node) {
        StringBuilder text = new StringBuilder();
        appendText(node, text);
        return text.toString();
    }

    private static void appendText(Node node, StringBuilder text) {
        if (node instanceof javafx.scene.control.Labeled labeled && labeled.getText() != null) {
            text.append(labeled.getText()).append('\n');
        }
        if (node instanceof Text textNode && textNode.getText() != null) {
            text.append(textNode.getText()).append('\n');
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            appendText(scrollPane.getContent(), text);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                appendText(child, text);
            }
        }
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
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

    private static final class MutableAiSettingsRepository implements AiSettingsRepository {
        private Optional<AiSettings> settings;

        private MutableAiSettingsRepository(Optional<AiSettings> settings) {
            this.settings = settings;
        }

        @Override
        public Optional<AiSettings> load() {
            return settings;
        }

        @Override
        public void save(AiSettings settings) {
            this.settings = Optional.of(settings);
        }
    }
}
