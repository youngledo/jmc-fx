package com.youngledo.jmcfx.ui.gc;

import java.time.ZoneId;
import java.util.List;

import com.youngledo.jmcfx.domain.model.G1GcRegionState;
import com.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR G1 GC split table/detail page.
public final class G1GcPageController {

    private final G1GcPageView view;
    private final I18n i18n;
    private final ChangeListener<G1GcRegionState> selectedRegionStateListener;
    private G1GcViewModel viewModel;

    public G1GcPageController(G1GcPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        selectedRegionStateListener = (observable, oldValue, newValue) ->
                view.regionStatesTable().getSelectionModel().select(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTables();
        bind(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.regionSummaryTable(), view.regionStatesTable(), view.pauseTable());
    }

    public void bind(G1GcViewModel nextViewModel) {
        G1GcViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.selectedRegionStateProperty().removeListener(selectedRegionStateListener);
        }
        view.summaryLabel().textProperty().unbind();
        view.detailArea().textProperty().unbind();
        view.regionSummaryTable().setItems(FXCollections.emptyObservableList());
        view.regionStatesTable().setItems(FXCollections.emptyObservableList());
        view.pauseTable().setItems(FXCollections.emptyObservableList());
        view.regionStatesTable().getSelectionModel().clearSelection();
        view.summaryLabel().setText(i18n.get("g1Gc.summary"));
        view.detailArea().setText("");
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.summaryLabel().textProperty().bind(nextViewModel.summaryProperty());
        view.detailArea().textProperty().bind(nextViewModel.selectedDetailProperty());
        view.regionSummaryTable().setItems(nextViewModel.regionSummariesProperty());
        view.regionStatesTable().setItems(nextViewModel.recentRegionStatesProperty());
        view.pauseTable().setItems(nextViewModel.gcPausesProperty());
        view.regionStatesTable().getSelectionModel().select(nextViewModel.selectedRegionStateProperty().get());
        nextViewModel.selectedRegionStateProperty().addListener(selectedRegionStateListener);
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("g1Gc.title"));
        view.regionStatesLabel().textProperty().bind(i18n.text("g1Gc.regionStates"));
        view.regionSummaryLabel().textProperty().bind(i18n.text("g1Gc.regionSummary"));
        view.pausesLabel().textProperty().bind(i18n.text("g1Gc.pauses"));
        view.detailTitleLabel().textProperty().bind(i18n.text("g1Gc.detail.title"));
    }

    private void configureTables() {
        view.regionSummaryTable().setPlaceholder(localizedTablePlaceholder("g1Gc.empty"));
        TableColumn<G1GcRegionSummary, String> typeCol = localizedColumn("g1Gc.column.type");
        typeCol.setPrefWidth(180);
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
        TableColumn<G1GcRegionSummary, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().regionCount())));
        TableColumn<G1GcRegionSummary, String> usedCol = localizedColumn("g1Gc.column.used");
        usedCol.setPrefWidth(140);
        usedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().usedBytes())));
        TableColumn<G1GcRegionSummary, String> capacityCol = localizedColumn("g1Gc.column.capacity");
        capacityCol.setPrefWidth(140);
        capacityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().capacityBytes())));
        view.regionSummaryTable().getColumns().setAll(List.of(typeCol, countCol, usedCol, capacityCol));

        view.regionStatesTable().setPlaceholder(localizedTablePlaceholder("g1Gc.empty"));
        TableColumn<G1GcRegionState, String> regionCol = localizedColumn("g1Gc.column.region");
        regionCol.setPrefWidth(90);
        regionCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().regionIndex())));
        TableColumn<G1GcRegionState, String> eventKindCol = localizedColumn("g1Gc.column.eventKind");
        eventKindCol.setPrefWidth(120);
        eventKindCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().eventKind()));
        TableColumn<G1GcRegionState, String> stateTypeCol = localizedColumn("g1Gc.column.type");
        stateTypeCol.setPrefWidth(160);
        stateTypeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
        TableColumn<G1GcRegionState, String> previousTypeCol = localizedColumn("g1Gc.column.previousType");
        previousTypeCol.setPrefWidth(160);
        previousTypeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().previousType()));
        TableColumn<G1GcRegionState, String> stateUsedCol = localizedColumn("g1Gc.column.used");
        stateUsedCol.setPrefWidth(140);
        stateUsedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().usedBytes())));
        TableColumn<G1GcRegionState, String> stateCapacityCol = localizedColumn("g1Gc.column.capacity");
        stateCapacityCol.setPrefWidth(140);
        stateCapacityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().capacityBytes())));
        TableColumn<G1GcRegionState, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.regionStatesTable().getColumns().setAll(List.of(regionCol, eventKindCol, stateTypeCol,
                previousTypeCol, stateUsedCol, stateCapacityCol, timeCol));
        view.regionStatesTable().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (viewModel != null && newValue != viewModel.selectedRegionStateProperty().get()) {
                        viewModel.selectedRegionStateProperty().set(newValue);
                    }
                });

        view.pauseTable().setPlaceholder(localizedTablePlaceholder("g1Gc.pauses.empty"));
        TableColumn<GcEvent, String> pauseIdCol = localizedColumn("gc.column.id");
        pauseIdCol.setPrefWidth(80);
        pauseIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcEvent, String> nameCol = localizedColumn("common.column.name");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<GcEvent, String> causeCol = localizedColumn("gcDetails.column.cause");
        causeCol.setPrefWidth(240);
        causeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cause()));
        TableColumn<GcEvent, String> totalPauseCol = localizedColumn("gcDetails.column.totalPause");
        totalPauseCol.setPrefWidth(120);
        totalPauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalPauseMicros())));
        TableColumn<GcEvent, String> pauseTimeCol = localizedColumn("common.column.time");
        pauseTimeCol.setPrefWidth(220);
        pauseTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.pauseTable().getColumns().setAll(List.of(pauseIdCol, nameCol, causeCol, totalPauseCol, pauseTimeCol));
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
}
