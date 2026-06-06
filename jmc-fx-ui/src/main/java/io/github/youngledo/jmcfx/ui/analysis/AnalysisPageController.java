package io.github.youngledo.jmcfx.ui.analysis;

import java.util.List;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.rules.RuleResultDetail;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;
import io.github.youngledo.jmcfx.ui.util.WorkbenchTableSupport;

import javafx.beans.binding.Bindings;
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
    private ObjectProperty<Integer> minimumScoreBinding;

    public AnalysisPageController(AnalysisPageView view, I18n i18n, Consumer<String> relatedPageNavigator) {
        this.view = view;
        this.i18n = i18n;
        this.relatedPageNavigator = relatedPageNavigator;
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
    }

    public void bind(RuleResultsViewModel nextViewModel) {
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
        }
        showDetail(null);
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.resultsProperty());
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
        view.table().getSelectionModel().selectFirst();
    }

    public TableView<RuleResult> table() {
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
        view.detailEvidenceCaption().textProperty().bind(i18n.text("analysis.detail.evidence"));
        view.detailRecommendationCaption().textProperty().bind(i18n.text("analysis.detail.recommendation"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("analysis.empty"));
        view.minimumScoreSpinner().setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-3, 100, 0));

        TableColumn<RuleResult, Severity> severityCol = new TableColumn<>();
        severityCol.textProperty().bind(i18n.text("analysis.column.severity"));
        severityCol.setPrefWidth(80);
        severityCol.setId("severity");
        severityCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().severity()));
        severityCol.setCellFactory(col -> new AnalysisSeverityCell<>());

        TableColumn<RuleResult, Number> scoreCol = new TableColumn<>();
        scoreCol.textProperty().bind(i18n.text("analysis.column.score"));
        scoreCol.setPrefWidth(72);
        scoreCol.setId("score");
        scoreCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().score()));
        useFormattedIntegerCells(scoreCol);

        TableColumn<RuleResult, String> pageCol = localizedColumn("analysis.column.rulePage");
        pageCol.setPrefWidth(140);
        pageCol.setId("rulePage");
        pageCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().topic()));

        TableColumn<RuleResult, String> resultIdCol = localizedColumn("analysis.column.resultId");
        resultIdCol.setPrefWidth(180);
        resultIdCol.setId("resultId");
        resultIdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));

        TableColumn<RuleResult, String> nameCol = localizedColumn("analysis.column.name");
        nameCol.setPrefWidth(280);
        nameCol.setId("rule");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<RuleResult, String> summaryCol = localizedColumn("analysis.column.summary");
        summaryCol.setPrefWidth(520);
        summaryCol.setId("summary");
        summaryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().summary()));

        view.table().getColumns().setAll(List.of(severityCol, scoreCol, pageCol, resultIdCol, nameCol, summaryCol));
        view.table().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (viewModel != null) {
                        viewModel.selectedResultProperty().set(val);
                    }
                    showDetail(val);
                });
        view.table().setRowFactory(table -> {
            TableRow<RuleResult> row = new TableRow<>();
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

    private void showDetail(RuleResult result) {
        RuleResultDetail detail = RuleResultDetail.from(result);
        if (detail == null) {
            view.detailExplanationArea().setText("");
            view.detailEvidenceArea().setText("");
            view.detailRecommendationArea().setText("");
            return;
        }
        view.detailExplanationArea().setText(detail.explanation());
        view.detailEvidenceArea().setText(detail.evidence());
        view.detailRecommendationArea().setText(detail.recommendation());
    }

    private void openRelatedPage(RuleResult result) {
        RuleResultDetail detail = RuleResultDetail.from(result);
        if (detail != null && detail.hasRelatedPage()) {
            relatedPageNavigator.accept(detail.relatedPageId());
        }
    }

    private Label localizedTablePlaceholder(String key) {
        return WorkbenchTableSupport.localizedPlaceholder(i18n, key);
    }

    private TableColumn<RuleResult, String> localizedColumn(String key) {
        TableColumn<RuleResult, String> column = new TableColumn<>();
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
