package io.github.youngledo.jmcfx.ui.heap;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR Heap data table and timeline page.
public final class HeapPageController {

    private final HeapPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private HeapViewModel viewModel;

    public HeapPageController(HeapPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> view.timelineChart().setData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        bind(null);
    }

    public TableView<HeapClassHistogram> table() {
        return view.table();
    }

    public void bind(HeapViewModel nextViewModel) {
        HeapViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.timelineProperty().removeListener(timelineListener);
        }
        view.table().setItems(FXCollections.emptyObservableList());
        view.timelineChart().setData(null);
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.histogramProperty());
        view.table().getSelectionModel().selectFirst();
        nextViewModel.timelineProperty().addListener(timelineListener);
        view.timelineChart().setData(nextViewModel.timelineProperty().get());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("heap.title"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("heap.empty"));

        TableColumn<HeapClassHistogram, String> classNameCol = new TableColumn<>();
        classNameCol.textProperty().bind(i18n.text("heap.column.className"));
        classNameCol.setPrefWidth(300);
        classNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().className()));

        TableColumn<HeapClassHistogram, Number> instancesCol = new TableColumn<>();
        instancesCol.textProperty().bind(i18n.text("heap.column.instances"));
        instancesCol.setPrefWidth(100);
        instancesCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().instances()));
        useFormattedIntegerCells(instancesCol);

        TableColumn<HeapClassHistogram, String> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(i18n.text("heap.column.size"));
        sizeCol.setPrefWidth(100);
        sizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().size())));

        TableColumn<HeapClassHistogram, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("heap.column.allocationPct"));
        pctCol.setPrefWidth(120);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().allocationPct())));

        view.table().getColumns().setAll(List.of(classNameCol, instancesCol, sizeCol, pctCol));
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
