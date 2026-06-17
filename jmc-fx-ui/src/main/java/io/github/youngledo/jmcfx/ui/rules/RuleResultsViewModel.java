package io.github.youngledo.jmcfx.ui.rules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.DiagnosticFindingsUseCase;
import io.github.youngledo.jmcfx.domain.model.DiagnosticEvidenceLink;
import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.ui.analysis.DiagnosticFindingDetail;
import io.github.youngledo.jmcfx.ui.detail.DetailSelection;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/// View model for JFR rule analysis results with severity filtering.
public class RuleResultsViewModel {

    private final AnalyzeRulesUseCase analyzeRules;
    private final DiagnosticFindingsUseCase diagnosticFindings;
    private final ObservableList<RuleResult> allResults = FXCollections.observableArrayList();
    private final FilteredList<RuleResult> results = new FilteredList<>(allResults);
    private final ObservableList<DiagnosticFinding> allFindings = FXCollections.observableArrayList();
    private final FilteredList<DiagnosticFinding> findings = new FilteredList<>(allFindings);
    private final ObjectProperty<RuleResult> selectedResult = new SimpleObjectProperty<>();
    private final ObjectProperty<RuleResultDetail> selectedDetail = new SimpleObjectProperty<>();
    private final ObjectProperty<DiagnosticFinding> selectedFinding = new SimpleObjectProperty<>();
    private final ObjectProperty<DiagnosticFindingDetail> selectedFindingDetail = new SimpleObjectProperty<>();
    private final ObjectProperty<DetailSelection> detailSelection = new SimpleObjectProperty<>();
    private final ObjectProperty<Set<Severity>> visibleSeverities =
            new SimpleObjectProperty<>(Set.of(Severity.WARNING, Severity.CRITICAL, Severity.INFO));
    private final IntegerProperty minimumScore = new SimpleIntegerProperty(0);
    private final StringProperty searchText = new SimpleStringProperty("");
    private final BooleanProperty showOkResults = new SimpleBooleanProperty(false);
    private final BooleanProperty showIgnoredResults = new SimpleBooleanProperty(false);
    private final BooleanProperty showUnavailableResults = new SimpleBooleanProperty(false);
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty loaded = new SimpleBooleanProperty(false);
    private final BooleanProperty error = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private RecordingSummary recording;
    private AiRecordingReport aiReport;

    public RuleResultsViewModel(AnalyzeRulesUseCase analyzeRules, DiagnosticFindingsUseCase diagnosticFindings) {
        this.analyzeRules = Objects.requireNonNull(analyzeRules, "analyzeRules");
        this.diagnosticFindings = Objects.requireNonNull(diagnosticFindings, "diagnosticFindings");
        visibleSeverities.addListener((obs, old, val) -> updateFilter());
        minimumScore.addListener((obs, old, val) -> updateFilter());
        searchText.addListener((obs, old, val) -> updateFilter());
        showOkResults.addListener((obs, old, val) -> updateFilter());
        showIgnoredResults.addListener((obs, old, val) -> updateFilter());
        showUnavailableResults.addListener((obs, old, val) -> updateFilter());
        selectedResult.addListener((obs, old, val) -> {
            selectedDetail.set(RuleResultDetail.from(val));
            detailSelection.set(detailSelectionFor(val));
        });
        selectedFinding.addListener((obs, old, val) ->
                selectedFindingDetail.set(DiagnosticFindingDetail.from(val)));
        updateFilter();
    }

    public ObservableList<RuleResult> resultsProperty() {
        return results;
    }

    public ObjectProperty<RuleResult> selectedResultProperty() {
        return selectedResult;
    }

    public ObjectProperty<RuleResultDetail> selectedDetailProperty() {
        return selectedDetail;
    }

    public ObservableList<DiagnosticFinding> findingsProperty() {
        return findings;
    }

    public ObjectProperty<DiagnosticFinding> selectedFindingProperty() {
        return selectedFinding;
    }

    public ObjectProperty<DiagnosticFindingDetail> selectedFindingDetailProperty() {
        return selectedFindingDetail;
    }

    public ObjectProperty<DetailSelection> detailSelectionProperty() {
        return detailSelection;
    }

    public ObjectProperty<Set<Severity>> visibleSeveritiesProperty() {
        return visibleSeverities;
    }

    public IntegerProperty minimumScoreProperty() {
        return minimumScore;
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public BooleanProperty showOkResultsProperty() {
        return showOkResults;
    }

    public BooleanProperty showIgnoredResultsProperty() {
        return showIgnoredResults;
    }

    public BooleanProperty showUnavailableResultsProperty() {
        return showUnavailableResults;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public BooleanProperty loadedProperty() {
        return loaded;
    }

    public BooleanProperty errorProperty() {
        return error;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public RecordingSummary recording() {
        return recording;
    }

    public void updateAiReport(AiRecordingReport report) {
        aiReport = report;
        refreshFindings();
    }

    public void analyze(RecordingSummary recording) {
        this.recording = recording;
        FxDispatch.run(() -> {
            loading.set(true);
            loaded.set(false);
            error.set(false);
            errorMessage.set("");
        });
        List<RuleResult> analyzed;
        try {
            analyzed = analyzeRules.analyze(recording);
        } catch (RuntimeException exception) {
            FxDispatch.run(() -> {
                loading.set(false);
                loaded.set(false);
                error.set(true);
                errorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                        : exception.getMessage());
            });
            throw exception;
        }
        FxDispatch.run(() -> {
            allResults.setAll(analyzed);
            aiReport = null;
            refreshFindings();
            updateFilter();
            selectedResult.set(results.isEmpty() ? null : results.getFirst());
            loaded.set(true);
            loading.set(false);
        });
    }

    private void updateFilter() {
        Set<Severity> visible = visibleSeverities.get();
        String query = normalizedSearchText();
        results.setPredicate(r -> isVisibleBySeverityAndScore(r, visible)
                && matchesSearch(r, query));
        findings.setPredicate(f -> isVisibleBySeverityAndScore(f, visible)
                && matchesSearch(f, query));
        refreshFilteredSelections();
    }

    private boolean isVisibleBySeverityAndScore(RuleResult result, Set<Severity> visible) {
        return isVisibleBySeverityAndScore(result.severity(), result.score(), visible);
    }

    private boolean isVisibleBySeverityAndScore(DiagnosticFinding finding, Set<Severity> visible) {
        return isVisibleBySeverityAndScore(finding.severity(), finding.score(), visible);
    }

    private boolean isVisibleBySeverityAndScore(Severity severity, int score, Set<Severity> visible) {
        if (severity == Severity.OK || severity == Severity.IGNORED || severity == Severity.UNAVAILABLE) {
            return isVisibleSeverity(severity, visible);
        }
        return isVisibleSeverity(severity, visible) && score >= minimumScore.get();
    }

    private boolean isVisibleSeverity(Severity severity, Set<Severity> visible) {
        if (severity == Severity.OK) {
            return showOkResults.get();
        }
        if (severity == Severity.IGNORED) {
            return showIgnoredResults.get();
        }
        if (severity == Severity.UNAVAILABLE) {
            return showUnavailableResults.get();
        }
        return visible == null || visible.isEmpty() || visible.contains(severity);
    }

    private String normalizedSearchText() {
        String text = searchText.get();
        return text == null ? "" : text.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean matchesSearch(RuleResult result, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(result.id(), query)
                || contains(result.name(), query)
                || contains(result.topic(), query)
                || contains(result.summary(), query)
                || contains(result.explanation(), query)
                || contains(result.evidence(), query)
                || contains(result.recommendation(), query)
                || contains(result.relatedPageId(), query);
    }

    private static boolean matchesSearch(DiagnosticFinding finding, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(finding.id(), query)
                || contains(finding.source().name(), query)
                || contains(finding.title(), query)
                || contains(finding.summary(), query)
                || contains(finding.explanation(), query)
                || contains(finding.recommendedNextAction(), query)
                || finding.evidenceLinks().stream().anyMatch(link -> matchesSearch(link, query));
    }

    private static boolean matchesSearch(DiagnosticEvidenceLink link, String query) {
        return contains(link.label(), query)
                || contains(link.relatedPageId(), query)
                || contains(link.relatedEntityId(), query)
                || contains(link.description(), query);
    }

    private void refreshFindings() {
        allFindings.setAll(diagnosticFindings.compose(allResults, Optional.ofNullable(aiReport)));
        selectedFinding.set(findings.isEmpty() ? null : findings.getFirst());
    }

    private void refreshFilteredSelections() {
        if (selectedResult.get() != null && !results.contains(selectedResult.get())) {
            selectedResult.set(results.isEmpty() ? null : results.getFirst());
        }
        if (selectedFinding.get() != null && !findings.contains(selectedFinding.get())) {
            selectedFinding.set(findings.isEmpty() ? null : findings.getFirst());
        }
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private static DetailSelection detailSelectionFor(RuleResult result) {
        if (result == null) {
            return null;
        }
        String meta = "%s | Score %d | %s".formatted(result.severity(), result.score(), result.topic());
        if (!result.relatedPageId().isBlank()) {
            meta = meta + " | " + result.relatedPageId();
        }
        return new DetailSelection("analysis", result.id(), result.name(), meta, detailBody(result));
    }

    private static String detailBody(RuleResult result) {
        StringBuilder body = new StringBuilder();
        appendSection(body, result.explanation());
        appendSection(body, result.evidence());
        appendSection(body, result.recommendation());
        return body.toString();
    }

    private static void appendSection(StringBuilder body, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!body.isEmpty()) {
            body.append("\n\n");
        }
        body.append(value);
    }
}
