package com.youngledo.jmcfx.ui.histogram;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/// Reusable histogram table component with dense layout and percentage bar support.
public class HistogramView<S> extends VBox {

    private final TableView<S> tableView;

    public HistogramView() {
        tableView = new TableView<>();
        tableView.getStyleClass().addAll("histogram-table", "dense-table");
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getChildren().setAll(tableView);
    }

    public TableView<S> getTableView() {
        return tableView;
    }

    public void setItems(ObservableList<S> items) {
        tableView.setItems(items);
    }

    public void addColumn(String label, double prefWidth,
            Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> cellValueFactory) {
        TableColumn<S, String> column = new TableColumn<>(label);
        column.setPrefWidth(prefWidth);
        column.setCellValueFactory(cellValueFactory);
        tableView.getColumns().add(column);
    }

    public void addPercentageColumn(String label, double prefWidth,
            Callback<TableColumn.CellDataFeatures<S, String>, ObservableValue<String>> cellValueFactory) {
        TableColumn<S, String> column = new TableColumn<>(label);
        column.setPrefWidth(prefWidth);
        column.setCellValueFactory(cellValueFactory);
        column.setCellFactory(col -> new PercentageBarTableCell<>());
        tableView.getColumns().add(column);
    }
}
