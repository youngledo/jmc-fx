package io.github.youngledo.jmcfx.ui.exceptions;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR Exceptions data table and timeline page.
public final class ExceptionsPageController {

    private final ExceptionsPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private ExceptionViewModel viewModel;

    public ExceptionsPageController(ExceptionsPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> view.timelineChart().setData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        bind(null);
    }

    public TableView<ExceptionSummary> table() {
        return view.table();
    }

    public void bind(ExceptionViewModel nextViewModel) {
        ExceptionViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.timelineProperty().removeListener(timelineListener);
        }
        view.timelineChart().setData(null);
        view.table().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.histogramProperty());
        nextViewModel.timelineProperty().addListener(timelineListener);
        view.timelineChart().setData(nextViewModel.timelineProperty().get());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("exceptions.title"));
        view.groupByClassButton().textProperty().bind(i18n.text("exceptions.grouping.byClass"));
        view.groupByMessageButton().textProperty().bind(i18n.text("exceptions.grouping.byMessage"));
        view.groupByClassAndMessageButton().textProperty().bind(i18n.text("exceptions.grouping.byClassAndMessage"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("exceptions.empty"));

        TableColumn<ExceptionSummary, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("exceptions.column.key"));
        keyCol.setPrefWidth(620);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<ExceptionSummary, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("exceptions.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<ExceptionSummary, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("exceptions.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        view.table().getColumns().setAll(List.of(keyCol, countCol, pctCol));

        view.groupByClassButton().setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS));
        view.groupByMessageButton().setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_MESSAGE));
        view.groupByClassAndMessageButton().setOnAction(
                event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS_AND_MESSAGE));
    }

    private void setExceptionGrouping(ExceptionGrouping grouping) {
        if (viewModel == null) {
            return;
        }
        viewModel.setGrouping(grouping);
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
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
