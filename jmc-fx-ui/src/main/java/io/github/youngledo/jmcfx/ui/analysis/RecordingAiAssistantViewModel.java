package io.github.youngledo.jmcfx.ui.analysis;

import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

import io.github.youngledo.jmcfx.application.ai.AnalyzeRecordingWithAiUseCase;
import io.github.youngledo.jmcfx.application.ai.AskRecordingAssistantUseCase;
import io.github.youngledo.jmcfx.application.ai.AiSettingsUseCase;
import io.github.youngledo.jmcfx.application.ai.BuildRecordingAiContextUseCase;
import io.github.youngledo.jmcfx.application.ai.RecordingAiContext;
import io.github.youngledo.jmcfx.application.ai.RecordingAiContextSection;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.ai.AiAssistantAnswer;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// Recording-scoped UI state for the offline AI assistant.
public final class RecordingAiAssistantViewModel implements AutoCloseable {

    private final BuildRecordingAiContextUseCase buildContextUseCase;
    private final AnalyzeRecordingWithAiUseCase analyzeUseCase;
    private final AskRecordingAssistantUseCase askUseCase;
    private final AiSettingsUseCase aiSettingsUseCase;
    private final RecordingAiAssistantExecutor executor;
    private final RecordingAiReportFormatter reportFormatter;
    private final LongSupplier nanoTime;
    private final ObservableList<AiFinding> findings = FXCollections.observableArrayList();
    private final ObservableList<AiAssistantAnswer> answers = FXCollections.observableArrayList();
    private final ObjectProperty<AiFinding> selectedFinding = new SimpleObjectProperty<>();
    private final ObjectProperty<AiRecordingReport> reportValue = new SimpleObjectProperty<>();
    private final BooleanProperty available = new SimpleBooleanProperty();
    private final BooleanProperty previewReady = new SimpleBooleanProperty();
    private final BooleanProperty confirmationRequired = new SimpleBooleanProperty(true);
    private final BooleanProperty analyzing = new SimpleBooleanProperty();
    private final BooleanProperty asking = new SimpleBooleanProperty();
    private final BooleanProperty reportReady = new SimpleBooleanProperty();
    private final BooleanProperty error = new SimpleBooleanProperty();
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty contextPreview = new SimpleStringProperty("");
    private final StringProperty streamingResponseText = new SimpleStringProperty("");
    private final StringProperty reportText = new SimpleStringProperty("");
    private final StringProperty reportSummary = new SimpleStringProperty("");
    private final StringProperty reportProcessingTime = new SimpleStringProperty("");
    private final StringProperty contextLimitations = new SimpleStringProperty("");
    private final StringProperty followUpQuestions = new SimpleStringProperty("");
    private final StringProperty questionText = new SimpleStringProperty("");
    private volatile RecordingAiContext context;
    private volatile AiRecordingReport report;
    private volatile RecordingSummary recording;
    private volatile long analysisStartNanos;

    public RecordingAiAssistantViewModel(BuildRecordingAiContextUseCase buildContextUseCase,
            AnalyzeRecordingWithAiUseCase analyzeUseCase,
            AskRecordingAssistantUseCase askUseCase,
            AiSettingsUseCase aiSettingsUseCase,
            RecordingAiAssistantExecutor executor,
            RecordingAiReportFormatter reportFormatter) {
        this(buildContextUseCase, analyzeUseCase, askUseCase, aiSettingsUseCase, executor, reportFormatter,
                System::nanoTime);
    }

    RecordingAiAssistantViewModel(BuildRecordingAiContextUseCase buildContextUseCase,
            AnalyzeRecordingWithAiUseCase analyzeUseCase,
            AskRecordingAssistantUseCase askUseCase,
            AiSettingsUseCase aiSettingsUseCase,
            RecordingAiAssistantExecutor executor,
            RecordingAiReportFormatter reportFormatter,
            LongSupplier nanoTime) {
        this.buildContextUseCase = buildContextUseCase;
        this.analyzeUseCase = analyzeUseCase;
        this.askUseCase = askUseCase;
        this.aiSettingsUseCase = aiSettingsUseCase;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.reportFormatter = Objects.requireNonNull(reportFormatter, "reportFormatter");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        available.set(currentAvailability());
    }

    public ObservableList<AiFinding> findingsProperty() {
        return findings;
    }

    public ObservableList<AiAssistantAnswer> answersProperty() {
        return answers;
    }

    public ObjectProperty<AiFinding> selectedFindingProperty() {
        return selectedFinding;
    }

    public ObjectProperty<AiRecordingReport> reportProperty() {
        return reportValue;
    }

    public BooleanProperty availableProperty() {
        return available;
    }

    public BooleanProperty previewReadyProperty() {
        return previewReady;
    }

    public BooleanProperty confirmationRequiredProperty() {
        return confirmationRequired;
    }

    public BooleanProperty analyzingProperty() {
        return analyzing;
    }

    public BooleanProperty askingProperty() {
        return asking;
    }

    public BooleanProperty reportReadyProperty() {
        return reportReady;
    }

    public BooleanProperty errorProperty() {
        return error;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public StringProperty contextPreviewProperty() {
        return contextPreview;
    }

    public StringProperty streamingResponseTextProperty() {
        return streamingResponseText;
    }

    public StringProperty reportTextProperty() {
        return reportText;
    }

    public StringProperty reportSummaryProperty() {
        return reportSummary;
    }

    public StringProperty reportProcessingTimeProperty() {
        return reportProcessingTime;
    }

    public StringProperty contextLimitationsProperty() {
        return contextLimitations;
    }

    public StringProperty followUpQuestionsProperty() {
        return followUpQuestions;
    }

    public StringProperty questionTextProperty() {
        return questionText;
    }

    public void setRecording(RecordingSummary recording) {
        if (!Objects.equals(this.recording, recording)) {
            resetForAnalysis();
        }
        this.recording = recording;
    }

    public void preparePreview() {
        RecordingSummary currentRecording = recording;
        if (currentRecording == null) {
            error.set(true);
            errorMessage.set("No recording is selected for AI analysis.");
            return;
        }
        preparePreview(currentRecording);
    }

    public void preparePreview(RecordingSummary nextRecording) {
        if (!providerAvailable()) {
            showUnavailable();
            return;
        }
        recording = Objects.requireNonNull(nextRecording, "recording");
        setBusy(true);
        executor.execute(() -> {
            try {
                RecordingAiContext builtContext = buildContextUseCase.build(nextRecording);
                FxDispatch.run(() -> {
                    context = builtContext;
                    contextPreview.set(previewText(builtContext));
                    previewReady.set(true);
                    error.set(false);
                    errorMessage.set("");
                    analyzing.set(false);
                });
            } catch (RuntimeException exception) {
                showFailure(exception);
            }
        });
    }

    public void analyze(String languageTag) {
        if (!providerAvailable()) {
            showUnavailable();
            return;
        }
        RecordingSummary currentRecording = recording;
        if (currentRecording == null) {
            error.set(true);
            errorMessage.set("No recording is selected for AI analysis.");
            return;
        }
        setBusy(true);
        analysisStartNanos = nanoTime.getAsLong();
        executor.execute(() -> {
            try {
                RecordingAiContext builtContext = buildContextUseCase.build(currentRecording);
                FxDispatch.run(() -> {
                    context = builtContext;
                    contextPreview.set(previewText(builtContext));
                    previewReady.set(true);
                    reportText.set("");
                    streamingResponseText.set("");
                });
                AiRecordingReport nextReport = analyzeUseCase.analyzeStreaming(builtContext, languageTag,
                        ignored -> { });
                FxDispatch.run(() -> showReport(nextReport));
            } catch (RuntimeException exception) {
                showFailure(exception);
            }
        });
    }

    public void ask(String languageTag) {
        if (askUseCase == null || context == null || report == null) {
            return;
        }
        String question = questionText.get();
        if (question == null || question.isBlank()) {
            return;
        }
        asking.set(true);
        error.set(false);
        executor.execute(() -> {
            try {
                AiAssistantAnswer answer = askUseCase.ask(context, report, question.strip(), languageTag);
                FxDispatch.run(() -> {
                    answers.add(answer);
                    questionText.set("");
                    followUpQuestions.set(join(answer.followUpQuestions()));
                    asking.set(false);
                });
            } catch (RuntimeException exception) {
                showFailure(exception);
                FxDispatch.run(() -> asking.set(false));
            }
        });
    }

    public void resetForAnalysis() {
        previewReady.set(false);
        reportReady.set(false);
        error.set(false);
        errorMessage.set("");
        streamingResponseText.set("");
        reportText.set("");
        reportProcessingTime.set("");
        reportValue.set(null);
        reportSummary.set("");
        contextLimitations.set("");
        followUpQuestions.set("");
        findings.clear();
        answers.clear();
        selectedFinding.set(null);
    }

    public void refreshAvailability() {
        boolean nextAvailable = currentAvailability();
        available.set(nextAvailable);
        if (nextAvailable && error.get() && errorMessage.get().contains("AI provider is not configured")) {
            error.set(false);
            errorMessage.set("");
        }
    }

    private void showUnavailable() {
        available.set(false);
        error.set(true);
        errorMessage.set("AI provider is not configured. Configure a model in Settings and make OPENAI_API_KEY "
                + "available in the process environment.");
    }

    private boolean providerAvailable() {
        boolean nextAvailable = currentAvailability();
        available.set(nextAvailable);
        return nextAvailable;
    }

    private boolean currentAvailability() {
        boolean providerConfigured = aiSettingsUseCase == null || aiSettingsUseCase.providerConfigured();
        return baseUseCasesAvailable() && providerConfigured;
    }

    private boolean baseUseCasesAvailable() {
        return buildContextUseCase != null && analyzeUseCase != null;
    }

    private void setBusy(boolean value) {
        analyzing.set(value);
        error.set(false);
        errorMessage.set("");
    }

    private void showReport(AiRecordingReport nextReport) {
        report = nextReport;
        streamingResponseText.set("");
        reportText.set(reportFormatter.reportText(nextReport));
        reportSummary.set(reportFormatter.summaryText(nextReport));
        reportProcessingTime.set(processingTimeText(nanoTime.getAsLong() - analysisStartNanos));
        findings.setAll(nextReport.findings());
        selectedFinding.set(findings.isEmpty() ? null : findings.getFirst());
        contextLimitations.set(join(nextReport.contextLimitations()));
        followUpQuestions.set(join(nextReport.followUpQuestions()));
        reportReady.set(true);
        previewReady.set(false);
        confirmationRequired.set(false);
        analyzing.set(false);
        error.set(false);
        errorMessage.set("");
        reportValue.set(nextReport);
    }

    private void showFailure(RuntimeException exception) {
        FxDispatch.run(() -> {
            analyzing.set(false);
            asking.set(false);
            error.set(true);
            errorMessage.set(message(exception));
        });
    }

    private static String previewText(RecordingAiContext context) {
        StringBuilder text = new StringBuilder();
        text.append("Recording: ").append(context.recording().name()).append('\n');
        text.append("Path: ").append(context.recording().path()).append('\n');
        text.append("Rule results sent: ").append(context.ruleResults().size()).append('\n');
        text.append("Context sections sent: ").append(context.sections().size()).append('\n');
        appendList(text, "Limitations", context.limitations());
        text.append("\nRule context:\n");
        for (RuleResult result : context.ruleResults()) {
            text.append("- [")
                    .append(result.severity())
                    .append("] ")
                    .append(result.name())
                    .append(" | score ")
                    .append(result.score())
                    .append(" | ")
                    .append(result.summary())
                    .append('\n');
        }
        appendSections(text, context.sections());
        return text.toString();
    }

    private static void appendList(StringBuilder text, String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        text.append("\n").append(title).append(":\n");
        for (String value : values) {
            text.append("- ").append(value).append('\n');
        }
    }

    private static void appendSections(StringBuilder text, List<RecordingAiContextSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        text.append("\nAdditional context:\n");
        for (RecordingAiContextSection section : sections) {
            text.append(section.title())
                    .append(" (")
                    .append(section.rows().size())
                    .append(" of ")
                    .append(section.totalCount())
                    .append("):\n");
            for (String row : section.rows()) {
                text.append("- ").append(row).append('\n');
            }
        }
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }

    private static String message(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String processingTimeText(long elapsedNanos) {
        long elapsedMillis = Math.max(0, elapsedNanos / 1_000_000L);
        if (elapsedMillis < 1_000) {
            return elapsedMillis + "ms";
        }
        long elapsedSeconds = Math.max(1, Math.round(elapsedMillis / 1_000.0));
        if (elapsedSeconds < 60) {
            return elapsedSeconds + "s";
        }
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        return seconds == 0 ? minutes + "m" : minutes + "m " + seconds + "s";
    }

    @Override
    public void close() {
        executor.close();
    }
}
