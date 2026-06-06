package io.github.youngledo.jmcfx.ui.heapdump;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectSummary;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;
import io.github.youngledo.jmcfx.ui.util.TableExportRegistration;
import io.github.youngledo.jmcfx.ui.util.TableExportRequests;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

/// Controller for the HPROF Heap Dump Analysis split table/detail page.
public final class HeapDumpAnalysisPageController {

    private final HeapDumpAnalysisPageView view;
    private final I18n i18n;
    private final ChangeListener<HeapDumpIssue> heapDumpTableSelectionListener =
            (observable, oldValue, newValue) -> selectHeapDumpIssue(newValue);
    private final ChangeListener<HeapDumpIssue> heapDumpSelectedIssueListener =
            (observable, oldValue, newValue) -> selectHeapDumpIssueInTable(newValue);
    private final ChangeListener<HeapDumpIssueCategory> categorySelectionListener =
            (observable, oldValue, newValue) -> selectHeapDumpCategory(newValue);
    private final ChangeListener<HeapDumpIssueCategory> selectedCategoryListener =
            (observable, oldValue, newValue) -> selectCategoryInCombo(newValue);
    private final ChangeListener<HeapDumpObjectGroup> objectGroupSelectionListener =
            (observable, oldValue, newValue) -> selectObjectGroup(newValue);
    private final ChangeListener<HeapDumpObjectGroup> selectedObjectGroupListener =
            (observable, oldValue, newValue) -> selectObjectGroupInTable(newValue);
    private HeapDumpAnalysisViewModel viewModel;

    public HeapDumpAnalysisPageController(HeapDumpAnalysisPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        bindLocalizedText();
        configureCategoryFilter();
        configureIssueTable();
        configureObjectGroupsTable();
        configureObjectGroupObjectsTable();
        configureReferencePathsTable();
        bind(null);
    }

    public List<TableExportRegistration> exportRegistrations() {
        return List.of(
                TableExportRequests.currentView(
                        view.issuesTable(),
                        "HPROF Heap Dump",
                        "Heap Dump Analysis",
                        "Issues",
                        this::heapDumpSource,
                        () -> null,
                        this::issueFilterSummary,
                        this::issueSelectionSummary),
                TableExportRequests.currentView(
                        view.objectGroupsTable(),
                        "HPROF Heap Dump",
                        "Heap Dump Analysis",
                        "Object Groups",
                        this::heapDumpSource,
                        () -> null,
                        () -> null,
                        this::objectGroupSelectionSummary),
                TableExportRequests.currentView(
                        view.objectGroupObjectsTable(),
                        "HPROF Heap Dump",
                        "Heap Dump Analysis",
                        "Object Group Objects",
                        this::heapDumpSource,
                        () -> null,
                        () -> null,
                        this::objectGroupObjectSelectionSummary),
                TableExportRequests.currentView(
                        view.referencePathsTable(),
                        "HPROF Heap Dump",
                        "Heap Dump Analysis",
                        "Reference Paths",
                        this::heapDumpSource,
                        () -> null,
                        () -> null,
                        this::referencePathSelectionSummary));
    }

    public void bind(HeapDumpAnalysisViewModel nextViewModel) {
        if (viewModel != null) {
            view.issuesTable().getSelectionModel().selectedItemProperty()
                    .removeListener(heapDumpTableSelectionListener);
            viewModel.selectedIssueProperty().removeListener(heapDumpSelectedIssueListener);
            view.categoryFilterCombo().getSelectionModel().selectedItemProperty()
                    .removeListener(categorySelectionListener);
            viewModel.selectedIssueCategoryProperty().removeListener(selectedCategoryListener);
            view.objectGroupsTable().getSelectionModel().selectedItemProperty()
                    .removeListener(objectGroupSelectionListener);
            viewModel.selectedObjectGroupProperty().removeListener(selectedObjectGroupListener);
        }
        view.issueDetailArea().textProperty().unbind();
        view.textReportArea().textProperty().unbind();
        view.loadReferencePathsButton().disableProperty().unbind();
        view.issueDetailTitleLabel().textProperty().unbind();
        view.objectGroupDetailTitleLabel().textProperty().unbind();
        view.objectGroupMetaLabel().textProperty().unbind();
        view.objectGroupDetailArea().textProperty().unbind();
        view.objectGroupObjectsTable().itemsProperty().unbind();
        view.objectGroupObjectsTable().setItems(FXCollections.emptyObservableList());
        if (nextViewModel == null) {
            viewModel = null;
            view.issueDetailArea().setText(i18n.get("heapDump.detail.empty"));
            view.objectGroupDetailTitleLabel().setText(i18n.get("heapDump.objectGroups.detail.emptyTitle"));
            view.objectGroupMetaLabel().setText("");
            view.objectGroupDetailArea().setText(i18n.get("heapDump.objectGroups.detail.empty"));
            view.objectGroupObjectsTable().setItems(FXCollections.emptyObservableList());
            view.textReportArea().setText("");
            view.issuesTable().setItems(FXCollections.emptyObservableList());
            view.objectGroupsTable().setItems(FXCollections.emptyObservableList());
            view.referencePathsTable().setItems(FXCollections.emptyObservableList());
            view.categoryFilterCombo().setItems(FXCollections.emptyObservableList());
            view.categoryFilterCombo().getSelectionModel().clearSelection();
            view.objectGroupsTable().getSelectionModel().clearSelection();
            view.loadReferencePathsButton().setDisable(true);
            view.issueDetailTitleLabel().setText("");
            return;
        }
        viewModel = nextViewModel;
        view.issueDetailArea().textProperty().bind(viewModel.selectedIssueDetailsProperty());
        view.textReportArea().textProperty().bind(viewModel.textReportProperty());
        view.issuesTable().setItems(viewModel.issues());
        view.objectGroupsTable().setItems(viewModel.objectGroups());
        view.referencePathsTable().setItems(viewModel.referencePaths());
        view.categoryFilterCombo().setItems(viewModel.issueCategories());
        view.issuesTable().getSelectionModel().selectedItemProperty()
                .addListener(heapDumpTableSelectionListener);
        viewModel.selectedIssueProperty().addListener(heapDumpSelectedIssueListener);
        view.categoryFilterCombo().getSelectionModel().selectedItemProperty()
                .addListener(categorySelectionListener);
        viewModel.selectedIssueCategoryProperty().addListener(selectedCategoryListener);
        view.objectGroupsTable().getSelectionModel().selectedItemProperty()
                .addListener(objectGroupSelectionListener);
        viewModel.selectedObjectGroupProperty().addListener(selectedObjectGroupListener);
        view.issueDetailTitleLabel().textProperty().bind(Bindings.createStringBinding(
                () -> {
                    HeapDumpIssue issue = viewModel.selectedIssueProperty().get();
                    return issue == null ? "" : issue.subject();
                },
                viewModel.selectedIssueProperty()));
        view.objectGroupDetailTitleLabel().textProperty().bind(Bindings.createStringBinding(
                () -> {
                    HeapDumpObjectGroup group = viewModel.selectedObjectGroupProperty().get();
                    return group == null ? i18n.get("heapDump.objectGroups.detail.emptyTitle") : group.label();
                },
                viewModel.selectedObjectGroupProperty(), i18n.localeProperty()));
        view.objectGroupMetaLabel().textProperty().bind(Bindings.createStringBinding(
                () -> formatObjectGroupMeta(viewModel.selectedObjectGroupProperty().get()),
                viewModel.selectedObjectGroupProperty(), i18n.localeProperty()));
        view.objectGroupDetailArea().textProperty().bind(viewModel.objectGroupStatusProperty());
        view.objectGroupObjectsTable().itemsProperty().bind(Bindings.createObjectBinding(
                () -> {
                    HeapDumpObjectGroupDetail detail = viewModel.selectedObjectGroupDetailProperty().get();
                    return detail == null
                            ? FXCollections.emptyObservableList()
                            : FXCollections.observableArrayList(detail.objects().rows());
                },
                viewModel.selectedObjectGroupDetailProperty()));
        view.loadReferencePathsButton().disableProperty().bind(viewModel.selectedObjectGroupProperty().isNull());
        selectHeapDumpIssueInTable(viewModel.selectedIssueProperty().get());
        selectObjectGroupInTable(viewModel.selectedObjectGroupProperty().get());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("heapDump.title"));
        view.categoryFilterCombo().promptTextProperty().bind(i18n.text("heapDump.filter.category"));
        view.clearCategoryFilterButton().textProperty().bind(i18n.text("heapDump.filter.clear"));
        view.loadReferencePathsButton().textProperty().bind(i18n.text("heapDump.referencePaths.load"));
        view.issueDetailTab().textProperty().bind(i18n.text("heapDump.detail.tab"));
        view.objectGroupsTab().textProperty().bind(i18n.text("heapDump.objectGroups.tab"));
        view.referencePathsTab().textProperty().bind(i18n.text("heapDump.referencePaths.tab"));
        view.textReportTab().textProperty().bind(i18n.text("heapDump.report.tab"));
    }

    private void configureCategoryFilter() {
        view.categoryFilterCombo().setConverter(new StringConverter<>() {
            @Override
            public String toString(HeapDumpIssueCategory category) {
                return category == null ? "" : categoryLabel(category);
            }

            @Override
            public HeapDumpIssueCategory fromString(String string) {
                return null;
            }
        });
        view.clearCategoryFilterButton().setOnAction(event -> clearCategoryFilter());
        view.loadReferencePathsButton().setOnAction(event -> loadReferencePaths());
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

    private void configureObjectGroupsTable() {
        view.objectGroupsTable().setPlaceholder(localizedTablePlaceholder("heapDump.objectGroups.empty"));

        TableColumn<HeapDumpObjectGroup, String> labelCol = localizedColumn("heapDump.objectGroups.column.label");
        labelCol.setPrefWidth(420);
        labelCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));

        TableColumn<HeapDumpObjectGroup, String> retainedCol = localizedColumn("heapDump.objectGroups.column.retained");
        retainedCol.setPrefWidth(130);
        retainedCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().retainedSizeBytes())));

        TableColumn<HeapDumpObjectGroup, String> shallowCol = localizedColumn("heapDump.objectGroups.column.shallow");
        shallowCol.setPrefWidth(130);
        shallowCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().shallowSizeBytes())));

        TableColumn<HeapDumpObjectGroup, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("heapDump.objectGroups.column.count"));
        countCol.setPrefWidth(110);
        countCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().objectCount()));
        useFormattedIntegerCells(countCol);

        view.objectGroupsTable().getColumns().setAll(List.of(labelCol, retainedCol, shallowCol, countCol));
    }

    private void configureObjectGroupObjectsTable() {
        view.objectGroupObjectsTable().setPlaceholder(localizedTablePlaceholder("heapDump.objectGroups.objects.empty"));

        TableColumn<HeapDumpObjectSummary, String> idCol = localizedColumn("heapDump.objectGroups.objects.column.id");
        idCol.setPrefWidth(150);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));

        TableColumn<HeapDumpObjectSummary, String> typeCol =
                localizedColumn("heapDump.objectGroups.objects.column.type");
        typeCol.setPrefWidth(360);
        typeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().typeName()));

        TableColumn<HeapDumpObjectSummary, String> shallowCol =
                localizedColumn("heapDump.objectGroups.objects.column.shallow");
        shallowCol.setPrefWidth(130);
        shallowCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().shallowSizeBytes())));

        TableColumn<HeapDumpObjectSummary, String> retainedCol =
                localizedColumn("heapDump.objectGroups.objects.column.retained");
        retainedCol.setPrefWidth(130);
        retainedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().retainedSizeAvailable()
                        ? DisplayFormats.formatFileSize(cell.getValue().retainedSizeBytes())
                        : ""));

        view.objectGroupObjectsTable().getColumns().setAll(List.of(idCol, typeCol, shallowCol, retainedCol));
    }

    private void configureReferencePathsTable() {
        view.referencePathsTable().setPlaceholder(localizedTablePlaceholder("heapDump.referencePaths.empty"));

        TableColumn<HeapDumpReferencePath, String> selectedCol =
                localizedColumn("heapDump.referencePaths.column.selected");
        selectedCol.setPrefWidth(150);
        selectedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().selectedObjectId()));

        TableColumn<HeapDumpReferencePath, String> retainedCol =
                localizedColumn("heapDump.referencePaths.column.retained");
        retainedCol.setPrefWidth(130);
        retainedCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().retainedSizeBytes())));

        TableColumn<HeapDumpReferencePath, String> pathCol =
                localizedColumn("heapDump.referencePaths.column.path");
        pathCol.setPrefWidth(420);
        pathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatReferencePath(cell.getValue())));

        TableColumn<HeapDumpReferencePath, String> stateCol =
                localizedColumn("heapDump.referencePaths.column.state");
        stateCol.setPrefWidth(100);
        stateCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().truncated() ? i18n.get("heapDump.referencePaths.truncated") : ""));

        view.referencePathsTable().getColumns().setAll(List.of(selectedCol, retainedCol, pathCol, stateCol));
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

    private void selectObjectGroup(HeapDumpObjectGroup group) {
        HeapDumpAnalysisViewModel viewModel = this.viewModel;
        if (viewModel != null) {
            viewModel.selectObjectGroup(group);
        }
    }

    private void selectObjectGroupInTable(HeapDumpObjectGroup group) {
        view.objectGroupsTable().getSelectionModel().select(group);
    }

    private void selectHeapDumpCategory(HeapDumpIssueCategory category) {
        HeapDumpAnalysisViewModel viewModel = this.viewModel;
        if (viewModel != null) {
            viewModel.selectIssueCategory(category);
        }
    }

    private void selectCategoryInCombo(HeapDumpIssueCategory category) {
        if (category == null) {
            view.categoryFilterCombo().getSelectionModel().clearSelection();
            return;
        }
        view.categoryFilterCombo().getSelectionModel().select(category);
    }

    private void clearCategoryFilter() {
        HeapDumpAnalysisViewModel viewModel = this.viewModel;
        if (viewModel != null) {
            viewModel.selectIssueCategory(null);
        }
    }

    private void loadReferencePaths() {
        HeapDumpAnalysisViewModel viewModel = this.viewModel;
        if (viewModel != null) {
            viewModel.loadReferencePaths(viewModel.selectedObjectGroupProperty().get());
        }
    }

    private String categoryLabel(HeapDumpIssueCategory category) {
        return i18n.get("heapDump.category." + category.name());
    }

    private String formatObjectGroupMeta(HeapDumpObjectGroup group) {
        if (group == null) {
            return "";
        }
        return i18n.format("heapDump.objectGroups.detail.meta",
                DisplayFormats.formatInteger(group.objectCount()),
                DisplayFormats.formatFileSize(group.retainedSizeBytes()),
                DisplayFormats.formatFileSize(group.shallowSizeBytes()));
    }

    private String formatReferencePath(HeapDumpReferencePath path) {
        if (path == null || path.edges().isEmpty()) {
            return "";
        }
        return path.edges().stream()
                .map(edge -> edge.sourceId() + " --" + edge.label() + "-> " + edge.targetId())
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private String heapDumpSource() {
        if (viewModel == null || viewModel.heapDumpNameProperty().get().isBlank()) {
            return "Heap Dump session";
        }
        return viewModel.heapDumpNameProperty().get();
    }

    private String issueFilterSummary() {
        HeapDumpIssueCategory category = viewModel == null ? null : viewModel.selectedIssueCategoryProperty().get();
        return category == null ? null : i18n.format("export.scope.categoryFilter", categoryLabel(category));
    }

    private String issueSelectionSummary() {
        int selectedCount = view.issuesTable().getSelectionModel().getSelectedItems().size();
        return selectedRowsSummary(selectedCount);
    }

    private String objectGroupSelectionSummary() {
        int selectedCount = view.objectGroupsTable().getSelectionModel().getSelectedItems().size();
        return selectedRowsSummary(selectedCount);
    }

    private String objectGroupObjectSelectionSummary() {
        int selectedCount = view.objectGroupObjectsTable().getSelectionModel().getSelectedItems().size();
        return selectedRowsSummary(selectedCount);
    }

    private String referencePathSelectionSummary() {
        int selectedCount = view.referencePathsTable().getSelectionModel().getSelectedItems().size();
        return selectedRowsSummary(selectedCount);
    }

    private String selectedRowsSummary(int selectedCount) {
        return selectedCount <= 0 ? null : i18n.format("export.scope.selectedRows", selectedCount);
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
