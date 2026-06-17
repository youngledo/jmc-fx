package io.github.youngledo.jmcfx.ui.analysis;

import java.util.List;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;
import io.github.youngledo.jmcfx.ui.util.WorkbenchTableSupport;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

/// Controller for the recording Analysis split table/detail page.
public final class AnalysisPageController {

    private final AnalysisPageView view;
    private final I18n i18n;
    private final Consumer<String> relatedPageNavigator;
    private RuleResultsViewModel viewModel;
    private RecordingAiAssistantViewModel aiViewModel;
    private ObjectProperty<Integer> minimumScoreBinding;
    private ChangeListener<DiagnosticFindingDetail> findingDetailListener;
    private ChangeListener<io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport> aiReportListener;
    private ChangeListener<java.util.Locale> aiLocaleListener;
    private InvalidationListener aiStateListener;

    public AnalysisPageController(AnalysisPageView view, I18n i18n, Consumer<String> relatedPageNavigator) {
        this.view = view;
        this.i18n = i18n;
        this.relatedPageNavigator = relatedPageNavigator;
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        configureAiAssistant();
    }

    public void bind(RuleResultsViewModel nextViewModel) {
        view.findingsSummaryLabel().textProperty().unbind();
        view.highestSeverityLabel().textProperty().unbind();
        view.ruleAnalysisStatusLabel().textProperty().unbind();
        view.table().placeholderProperty().unbind();
        view.table().setItems(FXCollections.emptyObservableList());
        if (viewModel != null) {
            view.searchField().textProperty().unbindBidirectional(viewModel.searchTextProperty());
            if (minimumScoreBinding != null) {
                view.minimumScoreSpinner().getValueFactory().valueProperty().unbindBidirectional(
                        minimumScoreBinding);
                minimumScoreBinding = null;
            }
            view.showOkCheckBox().selectedProperty().unbindBidirectional(viewModel.showOkResultsProperty());
            view.showIgnoredCheckBox().selectedProperty().unbindBidirectional(
                    viewModel.showIgnoredResultsProperty());
            view.showUnavailableCheckBox().selectedProperty().unbindBidirectional(
                    viewModel.showUnavailableResultsProperty());
            if (findingDetailListener != null) {
                viewModel.selectedFindingDetailProperty().removeListener(findingDetailListener);
                findingDetailListener = null;
            }
        }
        showDetail(null);
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            updateAiStatusLabel();
            return;
        }
        view.table().setItems(nextViewModel.findingsProperty());
        view.searchField().textProperty().bindBidirectional(nextViewModel.searchTextProperty());
        minimumScoreBinding = nextViewModel.minimumScoreProperty().asObject();
        view.minimumScoreSpinner().getValueFactory().valueProperty().bindBidirectional(
                minimumScoreBinding);
        view.showOkCheckBox().selectedProperty().bindBidirectional(nextViewModel.showOkResultsProperty());
        view.showIgnoredCheckBox().selectedProperty().bindBidirectional(nextViewModel.showIgnoredResultsProperty());
        view.showUnavailableCheckBox().selectedProperty().bindBidirectional(
                nextViewModel.showUnavailableResultsProperty());
        view.table().placeholderProperty().bind(Bindings.createObjectBinding(
                () -> analysisPlaceholder(nextViewModel),
                nextViewModel.loadingProperty(),
                nextViewModel.loadedProperty(),
                nextViewModel.errorProperty(),
                nextViewModel.errorMessageProperty(),
                i18n.localeProperty()));
        view.findingsSummaryLabel().textProperty().bind(Bindings.createStringBinding(
                () -> i18n.format("analysis.findings.summary", nextViewModel.findingsProperty().size()),
                nextViewModel.findingsProperty(),
                i18n.localeProperty()));
        view.highestSeverityLabel().textProperty().bind(Bindings.createStringBinding(
                () -> i18n.format("analysis.findings.highestSeverity", highestSeverity(nextViewModel)),
                nextViewModel.findingsProperty(),
                i18n.localeProperty()));
        view.ruleAnalysisStatusLabel().textProperty().bind(Bindings.createStringBinding(
                () -> i18n.format("analysis.findings.rules.status", ruleAnalysisStatus(nextViewModel)),
                nextViewModel.loadingProperty(),
                nextViewModel.loadedProperty(),
                nextViewModel.errorProperty(),
                i18n.localeProperty()));
        findingDetailListener = (observable, oldValue, newValue) -> showDetail(newValue);
        nextViewModel.selectedFindingDetailProperty().addListener(findingDetailListener);
        showDetail(nextViewModel.selectedFindingDetailProperty().get());
        updateAiStatusLabel();
        view.table().getSelectionModel().selectFirst();
    }

    public void bindAi(RecordingAiAssistantViewModel nextViewModel) {
        if (aiViewModel != null) {
            if (aiReportListener != null) {
                aiViewModel.reportProperty().removeListener(aiReportListener);
                aiReportListener = null;
            }
            if (aiLocaleListener != null) {
                i18n.localeProperty().removeListener(aiLocaleListener);
                aiLocaleListener = null;
            }
            if (aiStateListener != null) {
                aiViewModel.analyzingProperty().removeListener(aiStateListener);
                aiViewModel.askingProperty().removeListener(aiStateListener);
                aiViewModel.errorProperty().removeListener(aiStateListener);
                aiViewModel.errorMessageProperty().removeListener(aiStateListener);
                aiViewModel.reportReadyProperty().removeListener(aiStateListener);
                aiViewModel.reportProcessingTimeProperty().removeListener(aiStateListener);
                aiStateListener = null;
            }
            view.aiAnalyzeButton().disableProperty().unbind();
            unbindAiVisibility();
        }
        aiViewModel = nextViewModel;
        view.aiReportView().clear();
        if (nextViewModel == null) {
            view.aiReportView().showUnavailable(i18n);
            updateAiStatusLabel();
            return;
        }
        nextViewModel.refreshAvailability();
        aiReportListener = (observable, oldValue, newValue) -> {
            updateDiagnosticFindingsFromAiReport(newValue);
            refreshAiReportView(nextViewModel);
        };
        nextViewModel.reportProperty().addListener(aiReportListener);
        aiLocaleListener = (observable, oldValue, newValue) -> {
            refreshAiReportView(nextViewModel);
            updateAiStatusLabel();
        };
        i18n.localeProperty().addListener(aiLocaleListener);
        updateDiagnosticFindingsFromAiReport(nextViewModel.reportProperty().get());
        refreshAiReportView(nextViewModel);
        view.aiAnalyzeButton().disableProperty().bind(nextViewModel.availableProperty().not()
                .or(nextViewModel.analyzingProperty())
                .or(nextViewModel.askingProperty())
                .or(nextViewModel.reportReadyProperty()));
        aiStateListener = observable -> {
            refreshAiReportView(nextViewModel);
            updateAiStatusLabel();
        };
        nextViewModel.analyzingProperty().addListener(aiStateListener);
        nextViewModel.askingProperty().addListener(aiStateListener);
        nextViewModel.errorProperty().addListener(aiStateListener);
        nextViewModel.errorMessageProperty().addListener(aiStateListener);
        nextViewModel.reportReadyProperty().addListener(aiStateListener);
        nextViewModel.reportProcessingTimeProperty().addListener(aiStateListener);
        bindAiVisibility(nextViewModel);
        updateAiStatusLabel();
    }

    public void refreshAiAvailability() {
        if (aiViewModel != null) {
            aiViewModel.refreshAvailability();
        }
    }

    public TableView<DiagnosticFinding> table() {
        return view.table();
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("analysis.title"));
        view.searchField().promptTextProperty().bind(i18n.text("analysis.filter.search"));
        view.minimumScoreLabel().textProperty().bind(i18n.text("analysis.filter.minimumScore"));
        view.showOkCheckBox().textProperty().bind(i18n.text("analysis.filter.showOk"));
        view.showIgnoredCheckBox().textProperty().bind(i18n.text("analysis.filter.showIgnored"));
        view.showUnavailableCheckBox().textProperty().bind(i18n.text("analysis.filter.showUnavailable"));
        view.detailExplanationCaption().textProperty().bind(i18n.text("analysis.detail.explanation"));
        view.detailRecommendationCaption().textProperty().bind(i18n.text("analysis.detail.solution"));
        view.detailTabs().getTabs().get(0).textProperty().bind(i18n.text("analysis.detail.rulesTab"));
        view.detailTabs().getTabs().get(1).textProperty().bind(i18n.text("analysis.ai.tab"));
        view.aiTitleLabel().textProperty().bind(i18n.text("analysis.ai.title"));
        view.aiAnalyzeButton().textProperty().bind(i18n.text("analysis.ai.analyze"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("analysis.empty"));
        view.minimumScoreSpinner().setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-3, 100, 0));

        TableColumn<DiagnosticFinding, Severity> severityCol = new TableColumn<>();
        severityCol.textProperty().bind(i18n.text("analysis.column.severity"));
        severityCol.setPrefWidth(90);
        severityCol.setId("severity");
        severityCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().severity()));
        severityCol.setCellFactory(col -> new AnalysisSeverityCell<>());

        TableColumn<DiagnosticFinding, String> sourceCol = localizedColumn("analysis.column.source");
        sourceCol.setPrefWidth(90);
        sourceCol.setId("source");
        sourceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                i18n.get("analysis.source." + cell.getValue().source().name().toLowerCase(java.util.Locale.ROOT))));

        TableColumn<DiagnosticFinding, Number> scoreCol = new TableColumn<>();
        scoreCol.textProperty().bind(i18n.text("analysis.column.score"));
        scoreCol.setPrefWidth(72);
        scoreCol.setId("score");
        scoreCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().score()));
        useFormattedIntegerCells(scoreCol);

        TableColumn<DiagnosticFinding, String> findingCol = localizedColumn("analysis.column.finding");
        findingCol.setPrefWidth(300);
        findingCol.setId("finding");
        findingCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().title()));

        TableColumn<DiagnosticFinding, String> summaryCol = localizedColumn("analysis.column.summary");
        summaryCol.setPrefWidth(520);
        summaryCol.setId("summary");
        summaryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().summary()));

        view.table().getColumns().setAll(List.of(severityCol, sourceCol, scoreCol, findingCol, summaryCol));
        view.table().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (viewModel != null) {
                        viewModel.selectedFindingProperty().set(val);
                    }
                    showDetail(DiagnosticFindingDetail.from(val));
                });
        view.table().setRowFactory(table -> {
            TableRow<DiagnosticFinding> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {
                    openRelatedPage(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureAiAssistant() {
        view.aiAnalyzeButton().setOnAction(event -> {
            if (aiViewModel != null) {
                aiViewModel.resetForAnalysis();
                aiViewModel.analyze(i18n.localeProperty().get().toLanguageTag());
            }
        });
    }

    private void refreshAiReportView(RecordingAiAssistantViewModel model) {
        if (model == null || !model.availableProperty().get()) {
            view.aiReportView().showUnavailable(i18n);
            return;
        }
        if (model.analyzingProperty().get() || model.askingProperty().get()) {
            view.aiReportView().showStreamingResponse(i18n);
            return;
        }
        if (model.errorProperty().get()) {
            view.aiReportView().showError(aiErrorDetail(model));
            return;
        }
        view.aiReportView().showReport(model.reportProperty().get(), model.reportProcessingTimeProperty().get(), i18n,
                this::openAiRelatedPage);
    }

    private void updateDiagnosticFindingsFromAiReport(
            io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport report) {
        if (viewModel != null) {
            viewModel.updateAiReport(report);
        }
    }

    private void updateAiStatusLabel() {
        if (aiViewModel == null || !aiViewModel.availableProperty().get()) {
            view.aiStatusLabel().setText(i18n.get("analysis.findings.ai.unavailable"));
            return;
        }
        if (aiViewModel.analyzingProperty().get() || aiViewModel.askingProperty().get()) {
            view.aiStatusLabel().setText(i18n.get("analysis.findings.ai.running"));
            return;
        }
        if (aiViewModel.errorProperty().get()) {
            view.aiStatusLabel().setText(i18n.get("analysis.findings.ai.failed"));
            return;
        }
        view.aiStatusLabel().setText(aiViewModel.reportReadyProperty().get()
                ? i18n.get("analysis.findings.ai.ready")
                : i18n.get("analysis.findings.ai.notConfigured"));
    }

    private void bindAiVisibility(RecordingAiAssistantViewModel model) {
        view.aiAnalyzeButton().visibleProperty().bind(model.availableProperty());
        view.aiAnalyzeButton().managedProperty().bind(model.availableProperty());
        view.aiReportView().node().visibleProperty().bind(model.availableProperty());
        view.aiReportView().node().managedProperty().bind(model.availableProperty());
    }

    private void unbindAiVisibility() {
        for (javafx.scene.Node node : List.of(
                view.aiAnalyzeButton(),
                view.aiReportView().node())) {
            node.visibleProperty().unbind();
            node.managedProperty().unbind();
            node.setVisible(true);
            node.setManaged(true);
        }
    }

    private String aiErrorDetail(RecordingAiAssistantViewModel model) {
        return i18n.format("analysis.ai.failed", model.errorMessageProperty().get());
    }

    private Label analysisPlaceholder(RuleResultsViewModel nextViewModel) {
        if (nextViewModel.loadingProperty().get()) {
            return localizedTablePlaceholder("analysis.loading");
        }
        if (nextViewModel.errorProperty().get()) {
            Label label = new Label();
            label.setText(i18n.format("analysis.failed", nextViewModel.errorMessageProperty().get()));
            return label;
        }
        return localizedTablePlaceholder(nextViewModel.loadedProperty().get()
                ? "analysis.empty" : "analysis.loading");
    }

    private void showDetail(DiagnosticFindingDetail detail) {
        if (detail == null) {
            view.detailTitleLabel().setText(i18n.get("analysis.detail.noSelection"));
            view.detailMetaLabel().setText("");
            setDetailText(view.detailExplanationArea(), "");
            setDetailText(view.detailRecommendationArea(), "");
            return;
        }
        view.detailTitleLabel().setText(detail.title());
        view.detailMetaLabel().setText(detail.meta());
        setDetailText(view.detailExplanationArea(), detail.explanation());
        setDetailText(view.detailRecommendationArea(), detail.recommendation());
    }

    private static void setDetailText(Label label, String text) {
        label.setText(text == null ? "" : text);
    }

    private void openRelatedPage(DiagnosticFinding finding) {
        DiagnosticFindingDetail detail = DiagnosticFindingDetail.from(finding);
        if (detail != null && detail.hasRelatedPage()) {
            relatedPageNavigator.accept(detail.relatedPageId());
        }
    }

    void openSelectedFindingForTest() {
        openRelatedPage(view.table().getSelectionModel().getSelectedItem());
    }

    private void openAiRelatedPage(String relatedPageId) {
        if (relatedPageId != null && !relatedPageId.isBlank()) {
            relatedPageNavigator.accept(relatedPageId);
        }
    }

    private Label localizedTablePlaceholder(String key) {
        return WorkbenchTableSupport.localizedPlaceholder(i18n, key);
    }

    private TableColumn<DiagnosticFinding, String> localizedColumn(String key) {
        TableColumn<DiagnosticFinding, String> column = new TableColumn<>();
        column.textProperty().bind(i18n.text(key));
        return column;
    }

    private static String highestSeverity(RuleResultsViewModel model) {
        return model.findingsProperty().stream()
                .map(DiagnosticFinding::severity)
                .max(java.util.Comparator.comparingInt(AnalysisPageController::severityRank))
                .map(Enum::name)
                .orElse("UNKNOWN");
    }

    private String ruleAnalysisStatus(RuleResultsViewModel model) {
        if (model.errorProperty().get()) {
            return i18n.get("analysis.findings.rules.failed");
        }
        if (model.loadingProperty().get()) {
            return i18n.get("analysis.findings.rules.running");
        }
        if (model.loadedProperty().get()) {
            return i18n.get("analysis.findings.rules.ready");
        }
        return i18n.get("analysis.findings.rules.pending");
    }

    private static int severityRank(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 6;
            case WARNING -> 5;
            case INFO -> 4;
            case UNKNOWN -> 3;
            case OK -> 2;
            case UNAVAILABLE -> 1;
            case IGNORED -> 0;
        };
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
