package com.youngledo.jmcfx.ui.advanced;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.MemoryIssue;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AdvancedJfrViewModel {

    public static final int DEFAULT_BUCKET_COUNT = 20;
    public static final int DEFAULT_MAX_EVENT_TYPES = 12;
    public static final int DEFAULT_MAX_MEMORY_ISSUES = 12;

    private final AdvancedJfrAnalysisService service;
    private final ObjectProperty<EventHeatmap> heatmap = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHeatmapCell> selectedCell = new SimpleObjectProperty<>();
    private final ObjectProperty<MemoryAnalysisReport> memoryReport = new SimpleObjectProperty<>();
    private final ObservableList<MemoryIssue> memoryIssues = FXCollections.observableArrayList();
    private final ObjectProperty<MemoryIssue> selectedMemoryIssue = new SimpleObjectProperty<>();
    private final StringProperty summary = new SimpleStringProperty("");
    private final StringProperty selectedEventType = new SimpleStringProperty("");
    private final StringProperty selectedCount = new SimpleStringProperty("");
    private final StringProperty memorySummary = new SimpleStringProperty("");
    private final StringProperty selectedMemoryIssueTitle = new SimpleStringProperty("");
    private final StringProperty selectedMemoryIssueDetails = new SimpleStringProperty("");

    public AdvancedJfrViewModel(AdvancedJfrAnalysisService service) {
        this.service = service;
    }

    public ObjectProperty<EventHeatmap> heatmapProperty() {
        return heatmap;
    }

    public ObjectProperty<EventHeatmapCell> selectedCellProperty() {
        return selectedCell;
    }

    public StringProperty summaryProperty() {
        return summary;
    }

    public StringProperty selectedEventTypeProperty() {
        return selectedEventType;
    }

    public StringProperty selectedCountProperty() {
        return selectedCount;
    }

    public ObjectProperty<MemoryAnalysisReport> memoryReportProperty() {
        return memoryReport;
    }

    public ObservableList<MemoryIssue> memoryIssues() {
        return memoryIssues;
    }

    public ObjectProperty<MemoryIssue> selectedMemoryIssueProperty() {
        return selectedMemoryIssue;
    }

    public StringProperty memorySummaryProperty() {
        return memorySummary;
    }

    public StringProperty selectedMemoryIssueTitleProperty() {
        return selectedMemoryIssueTitle;
    }

    public StringProperty selectedMemoryIssueDetailsProperty() {
        return selectedMemoryIssueDetails;
    }

    public void load(RecordingSummary recording) {
        EventHeatmap loaded = service.loadEventHeatmap(recording, DEFAULT_BUCKET_COUNT, DEFAULT_MAX_EVENT_TYPES);
        MemoryAnalysisReport loadedMemoryReport = service.loadMemoryAnalysis(recording, DEFAULT_MAX_MEMORY_ISSUES);
        FxDispatch.run(() -> {
            heatmap.set(loaded);
            clearSelection();
            memoryReport.set(loadedMemoryReport);
            memoryIssues.setAll(loadedMemoryReport.issues());
            clearMemoryIssueSelection();
            long total = loaded.rows().stream().mapToLong(row -> row.totalCount()).sum();
            summary.set(loaded.rows().size() + " event types, " + DisplayFormats.formatInteger(total) + " events");
            memorySummary.set(formatMemorySummary(loadedMemoryReport));
        });
    }

    public void selectCell(EventHeatmapCell cell) {
        selectedCell.set(cell);
        if (cell == null) {
            clearSelection();
            return;
        }
        selectedEventType.set(cell.eventTypeId());
        selectedCount.set(DisplayFormats.formatInteger(cell.count()));
    }

    public void selectMemoryIssue(MemoryIssue issue) {
        selectedMemoryIssue.set(issue);
        if (issue == null) {
            clearMemoryIssueSelection();
            return;
        }
        selectedMemoryIssueTitle.set(issue.severity() + " - " + issue.subject());
        selectedMemoryIssueDetails.set(formatMemoryIssueDetails(issue));
    }

    private void clearSelection() {
        selectedCell.set(null);
        selectedEventType.set("");
        selectedCount.set("");
    }

    private void clearMemoryIssueSelection() {
        selectedMemoryIssue.set(null);
        selectedMemoryIssueTitle.set("");
        selectedMemoryIssueDetails.set("");
    }

    private String formatMemorySummary(MemoryAnalysisReport report) {
        return report.issues().size() + " " + issueLabel(report.issues().size())
                + ", " + DisplayFormats.formatFileSize(report.totalEstimatedBytes()) + " estimated"
                + ", " + DisplayFormats.formatInteger(report.totalCount()) + " events";
    }

    private String issueLabel(int issueCount) {
        return issueCount == 1 ? "issue" : "issues";
    }

    private String formatMemoryIssueDetails(MemoryIssue issue) {
        return """
                Category: %s
                Estimated bytes: %s
                Count: %s
                Score: %s
                Evidence: %s
                Recommendation: %s"""
                .formatted(issue.category(),
                        DisplayFormats.formatFileSize(issue.estimatedBytes()),
                        DisplayFormats.formatInteger(issue.count()),
                        DisplayFormats.formatPercent(issue.score()),
                        issue.evidence(),
                        issue.recommendation());
    }
}
