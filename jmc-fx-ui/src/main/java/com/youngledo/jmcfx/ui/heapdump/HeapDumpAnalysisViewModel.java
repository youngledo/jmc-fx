package com.youngledo.jmcfx.ui.heapdump;

import java.nio.file.Path;
import java.util.Objects;

import com.youngledo.jmcfx.application.AnalyzeHeapDumpUseCase;
import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import com.youngledo.jmcfx.domain.model.HeapDumpIssue;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class HeapDumpAnalysisViewModel {

    private final AnalyzeHeapDumpUseCase analyzeHeapDump;
    private final HeapDumpAnalysisExecutor executor;
    private final I18n i18n;
    private final ObjectProperty<HeapDumpAnalysisState> state =
            new SimpleObjectProperty<>(HeapDumpAnalysisState.IDLE);
    private final StringProperty heapDumpName = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty summary = new SimpleStringProperty("");
    private final ObjectProperty<HeapDumpAnalysisReport> report = new SimpleObjectProperty<>();
    private final ObservableList<HeapDumpIssue> issues = FXCollections.observableArrayList();
    private final ObjectProperty<HeapDumpIssue> selectedIssue = new SimpleObjectProperty<>();
    private final StringProperty selectedIssueDetails = new SimpleStringProperty("");
    private final StringProperty textReport = new SimpleStringProperty("");

    public HeapDumpAnalysisViewModel(AnalyzeHeapDumpUseCase analyzeHeapDump, HeapDumpAnalysisExecutor executor, I18n i18n) {
        this.analyzeHeapDump = Objects.requireNonNull(analyzeHeapDump, "analyzeHeapDump");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.i18n = Objects.requireNonNull(i18n, "i18n");
        statusMessage.set(i18n.get("heapDump.status.idle"));
        selectedIssueDetails.set(i18n.get("heapDump.detail.empty"));
    }

    public ObjectProperty<HeapDumpAnalysisState> stateProperty() {
        return state;
    }

    public StringProperty heapDumpNameProperty() {
        return heapDumpName;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public StringProperty summaryProperty() {
        return summary;
    }

    public ObjectProperty<HeapDumpAnalysisReport> reportProperty() {
        return report;
    }

    public ObservableList<HeapDumpIssue> issues() {
        return issues;
    }

    public ObjectProperty<HeapDumpIssue> selectedIssueProperty() {
        return selectedIssue;
    }

    public StringProperty selectedIssueDetailsProperty() {
        return selectedIssueDetails;
    }

    public StringProperty textReportProperty() {
        return textReport;
    }

    public void analyze(Path path) {
        Objects.requireNonNull(path, "path");
        String fileName = displayName(path);
        FxDispatch.run(() -> {
            state.set(HeapDumpAnalysisState.ANALYZING);
            heapDumpName.set(fileName);
            statusMessage.set(i18n.format("heapDump.status.analyzing", fileName));
            summary.set("");
        });
        executor.execute(() -> analyzeHeapDump.analyze(path), this::applyReport, this::applyFailure);
    }

    public void selectIssue(HeapDumpIssue issue) {
        FxDispatch.run(() -> {
            selectedIssue.set(issue);
            selectedIssueDetails.set(formatIssueDetails(issue));
        });
    }

    public void close() {
        executor.close();
    }

    private void applyReport(HeapDumpAnalysisReport nextReport) {
        FxDispatch.run(() -> {
            report.set(nextReport);
            issues.setAll(nextReport.issues());
            textReport.set(nextReport.textReport());
            state.set(HeapDumpAnalysisState.SUCCEEDED);
            statusMessage.set(i18n.format("heapDump.status.succeeded", displayName(nextReport.path())));
            summary.set(formatSummary(nextReport));
            selectIssue(issues.isEmpty() ? null : issues.getFirst());
        });
    }

    private void applyFailure(Throwable throwable) {
        String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
        FxDispatch.run(() -> {
            state.set(HeapDumpAnalysisState.FAILED);
            statusMessage.set(i18n.format("heapDump.status.failed", message));
            summary.set("");
            report.set(null);
            issues.clear();
            textReport.set("");
            selectIssue(null);
        });
    }

    private String formatSummary(HeapDumpAnalysisReport nextReport) {
        return i18n.format("heapDump.summary",
                DisplayFormats.formatFileSize(nextReport.fileSizeBytes()),
                DisplayFormats.formatFileSize(nextReport.totalObjectSizeBytes()),
                DisplayFormats.formatInteger(nextReport.objectCount()),
                DisplayFormats.formatInteger(nextReport.issues().size()));
    }

    private String formatIssueDetails(HeapDumpIssue issue) {
        if (issue == null) {
            return i18n.get("heapDump.detail.empty");
        }
        return String.join(System.lineSeparator(),
                i18n.format("heapDump.detail.category", issue.category()),
                i18n.format("heapDump.detail.wastedBytes", DisplayFormats.formatFileSize(issue.wastedBytes())),
                i18n.format("heapDump.detail.retainedBytes", DisplayFormats.formatFileSize(issue.retainedBytes())),
                i18n.format("heapDump.detail.objectCount", DisplayFormats.formatInteger(issue.objectCount())),
                i18n.format("heapDump.detail.score", DisplayFormats.formatPercent(issue.score() * 100.0)),
                i18n.format("heapDump.detail.evidence", issue.evidence()),
                i18n.format("heapDump.detail.referenceChain", issue.referenceChain()));
    }

    private String displayName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
