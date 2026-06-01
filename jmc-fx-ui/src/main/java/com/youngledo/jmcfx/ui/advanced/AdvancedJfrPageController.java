package com.youngledo.jmcfx.ui.advanced;

import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.MemoryIssue;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

/// Controller for the Advanced JFR tabbed heatmap and memory analysis page.
public final class AdvancedJfrPageController {

    private final AdvancedJfrPageView view;
    private final I18n i18n;
    private final EventHeatmapView heatmapView = new EventHeatmapView();
    private final ChangeListener<EventHeatmap> heatmapListener =
            (observable, oldValue, newValue) -> heatmapView.setHeatmap(newValue);
    private AdvancedJfrViewModel viewModel;
    private boolean rebindingMemorySelection;

    public AdvancedJfrPageController(AdvancedJfrPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        view.heatmapContainer().getChildren().setAll(heatmapView);
        bindLocalizedText();
        configureMemoryTable();
        bind(null);
    }

    public void bind(AdvancedJfrViewModel nextViewModel) {
        if (viewModel != null) {
            viewModel.heatmapProperty().removeListener(heatmapListener);
        }
        view.summaryLabel().textProperty().unbind();
        view.selectedEventTypeLabel().textProperty().unbind();
        view.selectedCountLabel().textProperty().unbind();
        view.memorySummaryLabel().textProperty().unbind();
        view.memoryDetailTitleLabel().textProperty().unbind();
        view.memoryDetailArea().textProperty().unbind();
        heatmapView.setHeatmap(null);
        heatmapView.setOnCellSelected(cell -> { });
        rebindingMemorySelection = true;
        try {
            view.memoryTable().setItems(FXCollections.emptyObservableList());
            view.memoryTable().getSelectionModel().clearSelection();
        } finally {
            rebindingMemorySelection = false;
        }
        view.memoryDetailTitleLabel().setText("");
        view.memoryDetailArea().setText("");
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            view.summaryLabel().setText(i18n.get("advancedJfr.summary"));
            view.selectedEventTypeLabel().setText("");
            view.selectedCountLabel().setText("");
            view.memorySummaryLabel().setText(i18n.get("advancedJfr.memory.summary"));
            return;
        }
        nextViewModel.heatmapProperty().addListener(heatmapListener);
        view.summaryLabel().textProperty().bind(nextViewModel.summaryProperty());
        view.selectedEventTypeLabel().textProperty().bind(nextViewModel.selectedEventTypeProperty());
        view.selectedCountLabel().textProperty().bind(nextViewModel.selectedCountProperty());
        view.memoryTable().setItems(nextViewModel.memoryIssues());
        bindMemoryText(nextViewModel);
        heatmapView.setOnCellSelected(nextViewModel::selectCell);
        heatmapView.setHeatmap(nextViewModel.heatmapProperty().get());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("advancedJfr.title"));
        view.heatmapTab().textProperty().bind(i18n.text("advancedJfr.heatmap.tab"));
        view.memoryTab().textProperty().bind(i18n.text("advancedJfr.memory.tab"));
        view.selectionTitleLabel().textProperty().bind(i18n.text("advancedJfr.selection.title"));
        view.selectedEventTypeCaptionLabel().textProperty().bind(i18n.text("advancedJfr.selection.eventType"));
        view.selectedCountCaptionLabel().textProperty().bind(i18n.text("advancedJfr.selection.count"));
    }

    private void configureMemoryTable() {
        view.memoryTable().setPlaceholder(localizedTablePlaceholder("advancedJfr.memory.empty"));

        TableColumn<MemoryIssue, String> severityCol = localizedColumn("advancedJfr.memory.column.severity");
        severityCol.setPrefWidth(110);
        severityCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().severity().name()));

        TableColumn<MemoryIssue, String> categoryCol = localizedColumn("advancedJfr.memory.column.category");
        categoryCol.setPrefWidth(180);
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category().name()));

        TableColumn<MemoryIssue, String> subjectCol = localizedColumn("advancedJfr.memory.column.subject");
        subjectCol.setPrefWidth(360);
        subjectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().subject()));

        TableColumn<MemoryIssue, String> estimatedBytesCol =
                localizedColumn("advancedJfr.memory.column.estimatedBytes");
        estimatedBytesCol.setPrefWidth(140);
        estimatedBytesCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().estimatedBytes())));

        TableColumn<MemoryIssue, String> countLabelCol =
                localizedColumn("advancedJfr.memory.column.count");
        TableColumn<MemoryIssue, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(countLabelCol.textProperty());
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<MemoryIssue, String> scoreCol = localizedColumn("advancedJfr.memory.column.score");
        scoreCol.setPrefWidth(90);
        scoreCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatPercent(cell.getValue().score())));

        view.memoryTable().getColumns().setAll(List.of(severityCol, categoryCol, subjectCol,
                estimatedBytesCol, countCol, scoreCol));
        view.memoryTable().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldIssue, issue) -> {
                    if (!rebindingMemorySelection && viewModel != null) {
                        viewModel.selectMemoryIssue(issue);
                    }
                });
    }

    private void bindMemoryText(AdvancedJfrViewModel nextViewModel) {
        view.memorySummaryLabel().textProperty().bind(Bindings.createStringBinding(
                () -> formatMemorySummary(nextViewModel.memoryReportProperty().get()),
                nextViewModel.memoryReportProperty(),
                i18n.localeProperty()));
        view.memoryDetailTitleLabel().textProperty().bind(Bindings.createStringBinding(
                () -> formatMemoryIssueTitle(nextViewModel.selectedMemoryIssueProperty().get()),
                nextViewModel.selectedMemoryIssueProperty(),
                i18n.localeProperty()));
        view.memoryDetailArea().textProperty().bind(Bindings.createStringBinding(
                () -> formatMemoryIssueDetails(nextViewModel.selectedMemoryIssueProperty().get()),
                nextViewModel.selectedMemoryIssueProperty(),
                i18n.localeProperty()));
    }

    private String formatMemorySummary(MemoryAnalysisReport report) {
        if (report == null) {
            return i18n.get("advancedJfr.memory.summary");
        }
        return i18n.format("advancedJfr.memory.summary.format",
                report.issues().size(),
                DisplayFormats.formatFileSize(report.totalEstimatedBytes()),
                DisplayFormats.formatInteger(report.totalCount()));
    }

    private String formatMemoryIssueTitle(MemoryIssue issue) {
        if (issue == null) {
            return "";
        }
        return i18n.format("advancedJfr.memory.detail.title", issue.severity(), issue.subject());
    }

    private String formatMemoryIssueDetails(MemoryIssue issue) {
        if (issue == null) {
            return "";
        }
        return String.join(System.lineSeparator(),
                i18n.format("advancedJfr.memory.detail.category", issue.category()),
                i18n.format("advancedJfr.memory.detail.estimatedBytes",
                        DisplayFormats.formatFileSize(issue.estimatedBytes())),
                i18n.format("advancedJfr.memory.detail.count", DisplayFormats.formatInteger(issue.count())),
                i18n.format("advancedJfr.memory.detail.score", DisplayFormats.formatPercent(issue.score())),
                i18n.format("advancedJfr.memory.detail.evidence", issue.evidence()),
                i18n.format("advancedJfr.memory.detail.recommendation", issue.recommendation()));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private <T> TableColumn<T, String> localizedColumn(String key) {
        TableColumn<T, String> column = new TableColumn<>();
        column.textProperty().bind(i18n.text(key));
        return column;
    }

    private static <T> void useFormattedIntegerCells(TableColumn<T, Number> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DisplayFormats.formatInteger(item.longValue()));
            }
        });
    }
}
