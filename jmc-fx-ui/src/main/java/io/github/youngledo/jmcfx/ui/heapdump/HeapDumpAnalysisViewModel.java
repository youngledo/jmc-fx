package io.github.youngledo.jmcfx.ui.heapdump;

import java.nio.file.Path;
import java.util.Objects;

import io.github.youngledo.jmcfx.application.AnalyzeHeapDumpUseCase;
import io.github.youngledo.jmcfx.application.BrowseHeapDumpObjectGroupsUseCase;
import io.github.youngledo.jmcfx.application.LoadHeapDumpObjectGroupDetailUseCase;
import io.github.youngledo.jmcfx.application.LoadHeapDumpReferencePathsUseCase;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseSort;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferenceDirection;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class HeapDumpAnalysisViewModel {

    private static final int DEFAULT_BROWSE_LIMIT = 100;
    private final AnalyzeHeapDumpUseCase analyzeHeapDump;
    private final BrowseHeapDumpObjectGroupsUseCase browseObjectGroups;
    private final LoadHeapDumpObjectGroupDetailUseCase loadObjectGroupDetail;
    private final LoadHeapDumpReferencePathsUseCase loadReferencePaths;
    private final HeapDumpAnalysisExecutor executor;
    private final I18n i18n;
    private final ObjectProperty<HeapDumpAnalysisState> state =
            new SimpleObjectProperty<>(HeapDumpAnalysisState.IDLE);
    private final StringProperty heapDumpName = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty summary = new SimpleStringProperty("");
    private final ObjectProperty<HeapDumpAnalysisReport> report = new SimpleObjectProperty<>();
    private final ObservableList<HeapDumpIssue> allIssues = FXCollections.observableArrayList();
    private final ObservableList<HeapDumpIssue> issues = FXCollections.observableArrayList();
    private final ObservableList<HeapDumpIssueCategory> issueCategories = FXCollections.observableArrayList();
    private final ObjectProperty<HeapDumpIssueCategory> selectedIssueCategory = new SimpleObjectProperty<>();
    private final ObjectProperty<HeapDumpIssue> selectedIssue = new SimpleObjectProperty<>();
    private final StringProperty selectedIssueDetails = new SimpleStringProperty("");
    private final StringProperty textReport = new SimpleStringProperty("");
    private final ObservableList<HeapDumpObjectGroup> objectGroups = FXCollections.observableArrayList();
    private final ObjectProperty<HeapDumpObjectGroup> selectedObjectGroup = new SimpleObjectProperty<>();
    private final ObjectProperty<HeapDumpObjectGroupDetail> selectedObjectGroupDetail = new SimpleObjectProperty<>();
    private final ObservableList<HeapDumpReferencePath> referencePaths = FXCollections.observableArrayList();
    private final StringProperty objectGroupStatus = new SimpleStringProperty("");
    private Path currentPath;

    public HeapDumpAnalysisViewModel(AnalyzeHeapDumpUseCase analyzeHeapDump, HeapDumpAnalysisExecutor executor, I18n i18n) {
        this(analyzeHeapDump, null, null, null, executor, i18n);
    }

    public HeapDumpAnalysisViewModel(AnalyzeHeapDumpUseCase analyzeHeapDump,
            BrowseHeapDumpObjectGroupsUseCase browseObjectGroups,
            LoadHeapDumpObjectGroupDetailUseCase loadObjectGroupDetail,
            LoadHeapDumpReferencePathsUseCase loadReferencePaths,
            HeapDumpAnalysisExecutor executor,
            I18n i18n) {
        this.analyzeHeapDump = Objects.requireNonNull(analyzeHeapDump, "analyzeHeapDump");
        this.browseObjectGroups = browseObjectGroups;
        this.loadObjectGroupDetail = loadObjectGroupDetail;
        this.loadReferencePaths = loadReferencePaths;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.i18n = Objects.requireNonNull(i18n, "i18n");
        statusMessage.set(i18n.get("heapDump.status.idle"));
        selectedIssueDetails.set(i18n.get("heapDump.detail.empty"));
        objectGroupStatus.set(i18n.get("heapDump.detail.empty"));
        selectedIssueCategory.addListener((observable, oldValue, newValue) -> applyIssueFilter());
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

    public ObservableList<HeapDumpIssueCategory> issueCategories() {
        return issueCategories;
    }

    public ObjectProperty<HeapDumpIssueCategory> selectedIssueCategoryProperty() {
        return selectedIssueCategory;
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

    public ObservableList<HeapDumpObjectGroup> objectGroups() {
        return objectGroups;
    }

    public ObjectProperty<HeapDumpObjectGroup> selectedObjectGroupProperty() {
        return selectedObjectGroup;
    }

    public ObjectProperty<HeapDumpObjectGroupDetail> selectedObjectGroupDetailProperty() {
        return selectedObjectGroupDetail;
    }

    public ObservableList<HeapDumpReferencePath> referencePaths() {
        return referencePaths;
    }

    public StringProperty objectGroupStatusProperty() {
        return objectGroupStatus;
    }

    public void analyze(Path path) {
        Objects.requireNonNull(path, "path");
        String fileName = displayName(path);
        FxDispatch.run(() -> {
            currentPath = path;
            state.set(HeapDumpAnalysisState.ANALYZING);
            heapDumpName.set(fileName);
            statusMessage.set(i18n.format("heapDump.status.analyzing", fileName));
            summary.set("");
            objectGroups.clear();
            selectedObjectGroup.set(null);
            selectedObjectGroupDetail.set(null);
            referencePaths.clear();
        });
        executor.execute(() -> analyzeHeapDump.analyze(path), this::applyReport, this::applyFailure);
    }

    public void selectIssue(HeapDumpIssue issue) {
        FxDispatch.run(() -> {
            selectedIssue.set(issue);
            selectedIssueDetails.set(formatIssueDetails(issue));
        });
    }

    public void selectIssueCategory(HeapDumpIssueCategory category) {
        FxDispatch.run(() -> selectedIssueCategory.set(category));
    }

    public void loadObjectGroups() {
        if (!browsingAvailable()) {
            FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.browsing.unavailable")));
            return;
        }
        Path path = currentPath;
        if (path == null) {
            FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.openPrompt")));
            return;
        }
        HeapDumpBrowseRequest request = browseRequest(path);
        FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.objectGroups.loading")));
        executor.execute(() -> browseObjectGroups.browse(request), this::applyObjectGroups, this::applyObjectGroupFailure);
    }

    public void selectObjectGroup(HeapDumpObjectGroup group) {
        FxDispatch.run(() -> selectedObjectGroup.set(group));
        if (group == null) {
            FxDispatch.run(() -> {
                selectedObjectGroupDetail.set(null);
                referencePaths.clear();
                objectGroupStatus.set(i18n.get("heapDump.detail.empty"));
            });
            return;
        }
        if (!browsingAvailable()) {
            FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.browsing.unavailable")));
            return;
        }
        Path path = currentPath;
        if (path == null) {
            FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.openPrompt")));
            return;
        }
        HeapDumpBrowseRequest request = browseRequest(path);
        FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.objectGroups.detail.loading")));
        executor.execute(() -> loadObjectGroupDetail.load(request, group.id()), this::applyObjectGroupDetail,
                this::applyObjectGroupFailure);
    }

    public void loadReferencePaths(HeapDumpObjectGroup group) {
        if (group == null || loadReferencePaths == null) {
            FxDispatch.run(referencePaths::clear);
            return;
        }
        Path path = currentPath;
        if (path == null) {
            FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.openPrompt")));
            return;
        }
        HeapDumpReferencePathRequest request = new HeapDumpReferencePathRequest(path, group.id(),
                HeapDumpReferenceDirection.INBOUND, 8, DEFAULT_BROWSE_LIMIT, 0, DEFAULT_BROWSE_LIMIT);
        FxDispatch.run(() -> objectGroupStatus.set(i18n.get("heapDump.referencePaths.loading")));
        executor.execute(() -> loadReferencePaths.load(request), this::applyReferencePaths, this::applyObjectGroupFailure);
    }

    public void close() {
        executor.close();
    }

    private void applyReport(HeapDumpAnalysisReport nextReport) {
        FxDispatch.run(() -> {
            report.set(nextReport);
            allIssues.setAll(nextReport.issues());
            issueCategories.setAll(nextReport.issues().stream()
                    .map(HeapDumpIssue::category)
                    .distinct()
                    .toList());
            applyIssueFilter();
            textReport.set(nextReport.textReport());
            state.set(HeapDumpAnalysisState.SUCCEEDED);
            statusMessage.set(i18n.format("heapDump.status.succeeded", displayName(nextReport.path())));
            summary.set(formatSummary(nextReport));
            selectIssue(issues.isEmpty() ? null : issues.getFirst());
        });
        loadObjectGroups();
    }

    private void applyFailure(Throwable throwable) {
        String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
        FxDispatch.run(() -> {
            state.set(HeapDumpAnalysisState.FAILED);
            statusMessage.set(i18n.format("heapDump.status.failed", message));
            summary.set("");
            report.set(null);
            allIssues.clear();
            issues.clear();
            issueCategories.clear();
            selectedIssueCategory.set(null);
            textReport.set("");
            objectGroups.clear();
            selectedObjectGroup.set(null);
            selectedObjectGroupDetail.set(null);
            referencePaths.clear();
            objectGroupStatus.set(message);
            selectIssue(null);
        });
    }

    private void applyIssueFilter() {
        HeapDumpIssueCategory category = selectedIssueCategory.get();
        if (category == null) {
            issues.setAll(allIssues);
        } else {
            issues.setAll(allIssues.stream()
                    .filter(issue -> issue.category() == category)
                    .toList());
        }
        selectIssue(issues.isEmpty() ? null : issues.getFirst());
    }

    private void applyObjectGroups(HeapDumpBrowseWindow<HeapDumpObjectGroup> window) {
        FxDispatch.run(() -> {
            objectGroups.setAll(window.rows());
            if (window.rows().isEmpty() && window.truncated()) {
                objectGroupStatus.set(i18n.get("heapDump.browsing.unavailable"));
            } else {
                objectGroupStatus.set(window.truncated()
                        ? i18n.format("heapDump.objectGroups.loaded.truncated", window.rows().size())
                        : i18n.format("heapDump.objectGroups.loaded", window.rows().size()));
            }
            selectObjectGroup(objectGroups.isEmpty() ? null : objectGroups.getFirst());
        });
    }

    private void applyObjectGroupDetail(HeapDumpObjectGroupDetail detail) {
        FxDispatch.run(() -> {
            selectedObjectGroupDetail.set(detail);
            objectGroupStatus.set(detail.note().isBlank() ? detail.group().label() : detail.note());
        });
    }

    private void applyReferencePaths(HeapDumpBrowseWindow<HeapDumpReferencePath> window) {
        FxDispatch.run(() -> {
            referencePaths.setAll(window.rows());
            if (window.rows().isEmpty() && window.truncated()) {
                objectGroupStatus.set(i18n.get("heapDump.referencePaths.unavailable"));
            } else {
                objectGroupStatus.set(window.truncated()
                        ? i18n.format("heapDump.referencePaths.loaded.truncated", window.rows().size())
                        : i18n.format("heapDump.referencePaths.loaded", window.rows().size()));
            }
        });
    }

    private void applyObjectGroupFailure(Throwable throwable) {
        String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
        FxDispatch.run(() -> objectGroupStatus.set(message));
    }

    private boolean browsingAvailable() {
        return browseObjectGroups != null && loadObjectGroupDetail != null;
    }

    private HeapDumpBrowseRequest browseRequest(Path path) {
        return new HeapDumpBrowseRequest(path, HeapDumpObjectGroupKind.CLASS, HeapDumpBrowseSort.RETAINED_SIZE_BYTES,
                false, 0, DEFAULT_BROWSE_LIMIT, "");
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
