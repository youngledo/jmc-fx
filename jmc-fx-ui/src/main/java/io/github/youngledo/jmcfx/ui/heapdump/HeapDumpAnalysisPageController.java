package io.github.youngledo.jmcfx.ui.heapdump;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

/// Controller for the HPROF Heap Dump Analysis split table/detail page.
public final class HeapDumpAnalysisPageController {

    private final HeapDumpAnalysisPageView view;
    private final I18n i18n;
    private final ChangeListener<HeapDumpIssue> heapDumpTableSelectionListener =
            (observable, oldValue, newValue) -> selectHeapDumpIssue(newValue);
    private final ChangeListener<HeapDumpIssue> heapDumpSelectedIssueListener =
            (observable, oldValue, newValue) -> selectHeapDumpIssueInTable(newValue);
    private HeapDumpAnalysisViewModel viewModel;

    public HeapDumpAnalysisPageController(HeapDumpAnalysisPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        bindLocalizedText();
        configureIssueTable();
        bind(null);
    }

    public void bind(HeapDumpAnalysisViewModel nextViewModel) {
        if (viewModel != null) {
            view.issuesTable().getSelectionModel().selectedItemProperty()
                    .removeListener(heapDumpTableSelectionListener);
            viewModel.selectedIssueProperty().removeListener(heapDumpSelectedIssueListener);
        }
        view.issueDetailArea().textProperty().unbind();
        view.textReportArea().textProperty().unbind();
        view.issueDetailTitleLabel().textProperty().unbind();
        if (nextViewModel == null) {
            viewModel = null;
            view.issueDetailArea().setText(i18n.get("heapDump.detail.empty"));
            view.textReportArea().setText("");
            view.issuesTable().setItems(FXCollections.emptyObservableList());
            view.issueDetailTitleLabel().setText("");
            return;
        }
        viewModel = nextViewModel;
        view.issueDetailArea().textProperty().bind(viewModel.selectedIssueDetailsProperty());
        view.textReportArea().textProperty().bind(viewModel.textReportProperty());
        view.issuesTable().setItems(viewModel.issues());
        view.issuesTable().getSelectionModel().selectedItemProperty()
                .addListener(heapDumpTableSelectionListener);
        viewModel.selectedIssueProperty().addListener(heapDumpSelectedIssueListener);
        view.issueDetailTitleLabel().textProperty().bind(Bindings.createStringBinding(
                () -> {
                    HeapDumpIssue issue = viewModel.selectedIssueProperty().get();
                    return issue == null ? "" : issue.subject();
                },
                viewModel.selectedIssueProperty()));
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("heapDump.title"));
        view.issueDetailTab().textProperty().bind(i18n.text("heapDump.detail.tab"));
        view.textReportTab().textProperty().bind(i18n.text("heapDump.report.tab"));
    }

    private void configureIssueTable() {
        view.issuesTable().setPlaceholder(localizedTablePlaceholder("heapDump.openPrompt"));

        TableColumn<HeapDumpIssue, String> categoryCol = localizedColumn("heapDump.column.category");
        categoryCol.setPrefWidth(190);
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category().name()));

        TableColumn<HeapDumpIssue, String> subjectCol = localizedColumn("heapDump.column.subject");
        subjectCol.setPrefWidth(440);
        subjectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().subject()));

        TableColumn<HeapDumpIssue, String> wastedBytesCol = localizedColumn("heapDump.column.wastedBytes");
        wastedBytesCol.setPrefWidth(120);
        wastedBytesCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().wastedBytes())));

        TableColumn<HeapDumpIssue, Number> objectCountCol = new TableColumn<>();
        objectCountCol.textProperty().bind(i18n.text("heapDump.column.objectCount"));
        objectCountCol.setPrefWidth(110);
        objectCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().objectCount()));
        useFormattedIntegerCells(objectCountCol);

        TableColumn<HeapDumpIssue, String> scoreCol = localizedColumn("heapDump.column.score");
        scoreCol.setPrefWidth(90);
        scoreCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatPercent(cell.getValue().score() * 100.0)));

        view.issuesTable().getColumns().setAll(List.of(categoryCol, subjectCol, wastedBytesCol,
                objectCountCol, scoreCol));
    }

    private void selectHeapDumpIssue(HeapDumpIssue issue) {
        HeapDumpAnalysisViewModel viewModel = this.viewModel;
        if (viewModel != null) {
            viewModel.selectIssue(issue);
        }
    }

    private void selectHeapDumpIssueInTable(HeapDumpIssue issue) {
        view.issuesTable().getSelectionModel().select(issue);
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
