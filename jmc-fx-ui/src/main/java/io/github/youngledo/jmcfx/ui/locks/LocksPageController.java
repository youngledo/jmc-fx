package io.github.youngledo.jmcfx.ui.locks;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR Locks tabbed data page.
public final class LocksPageController {

    private final LocksPageView view;
    private final I18n i18n;
    private LockViewModel viewModel;

    public LocksPageController(LocksPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        bindLocalizedText();
        configureTables();
        bind(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.byClassTable(), view.byAddressTable(), view.byThreadTable());
    }

    public void bind(LockViewModel nextViewModel) {
        view.byClassTable().setItems(FXCollections.emptyObservableList());
        view.byAddressTable().setItems(FXCollections.emptyObservableList());
        view.byThreadTable().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.byClassTable().setItems(nextViewModel.classHistogramProperty());
        view.byAddressTable().setItems(nextViewModel.addressHistogramProperty());
        view.byThreadTable().setItems(nextViewModel.threadHistogramProperty());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("locks.title"));
        view.groupByClassButton().textProperty().bind(i18n.text("locks.grouping.byClass"));
        view.groupByAddressButton().textProperty().bind(i18n.text("locks.grouping.byAddress"));
        view.groupByThreadButton().textProperty().bind(i18n.text("locks.grouping.byThread"));
        view.byClassTab().textProperty().bind(i18n.text("locks.tab.byClass"));
        view.byAddressTab().textProperty().bind(i18n.text("locks.tab.byAddress"));
        view.byThreadTab().textProperty().bind(i18n.text("locks.tab.byThread"));
    }

    private void configureTables() {
        configureSingleLockTable(view.byClassTable(), "locks.empty");
        configureSingleLockTable(view.byAddressTable(), "locks.empty");
        configureSingleLockTable(view.byThreadTable(), "locks.empty");

        view.groupByClassButton().setOnAction(event -> setPrimaryGrouping(LockGrouping.BY_CLASS));
        view.groupByAddressButton().setOnAction(event -> setPrimaryGrouping(LockGrouping.BY_ADDRESS));
        view.groupByThreadButton().setOnAction(event -> setPrimaryGrouping(LockGrouping.BY_THREAD));
    }

    private void configureSingleLockTable(TableView<LockHistogram> table, String emptyKey) {
        table.setPlaceholder(localizedTablePlaceholder(emptyKey));

        TableColumn<LockHistogram, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("locks.column.key"));
        keyCol.setPrefWidth(520);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<LockHistogram, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("locks.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<LockHistogram, String> totalDurCol = new TableColumn<>();
        totalDurCol.textProperty().bind(i18n.text("locks.column.totalDuration"));
        totalDurCol.setPrefWidth(120);
        totalDurCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().totalDuration())));

        TableColumn<LockHistogram, String> maxDurCol = new TableColumn<>();
        maxDurCol.textProperty().bind(i18n.text("locks.column.maxDuration"));
        maxDurCol.setPrefWidth(120);
        maxDurCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().maxDuration())));

        TableColumn<LockHistogram, String> avgDurCol = new TableColumn<>();
        avgDurCol.textProperty().bind(i18n.text("locks.column.avgDuration"));
        avgDurCol.setPrefWidth(120);
        avgDurCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().avgDuration())));

        table.getColumns().setAll(List.of(keyCol, countCol, totalDurCol, maxDurCol, avgDurCol));
    }

    private void setPrimaryGrouping(LockGrouping grouping) {
        if (viewModel == null) {
            return;
        }
        viewModel.setPrimaryGrouping(grouping);
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
