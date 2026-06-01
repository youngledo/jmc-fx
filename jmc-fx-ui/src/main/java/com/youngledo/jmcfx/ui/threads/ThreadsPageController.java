package com.youngledo.jmcfx.ui.threads;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR Thread Activity data table page.
public final class ThreadsPageController {

    private final ThreadsPageView view;
    private final I18n i18n;
    private ThreadViewModel viewModel;

    public ThreadsPageController(ThreadsPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        bind(null);
    }

    public TableView<ThreadSummary> table() {
        return view.table();
    }

    public void bind(ThreadViewModel nextViewModel) {
        view.table().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.threadSummariesProperty());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("threads.title"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("threads.empty"));

        TableColumn<ThreadSummary, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("threads.column.name"));
        nameCol.setPrefWidth(520);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        TableColumn<ThreadSummary, Number> samplesCol = new TableColumn<>();
        samplesCol.textProperty().bind(i18n.text("threads.column.samples"));
        samplesCol.setPrefWidth(100);
        samplesCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().sampleCount()));
        useFormattedIntegerCells(samplesCol);

        TableColumn<ThreadSummary, String> blockedCol = new TableColumn<>();
        blockedCol.textProperty().bind(i18n.text("threads.column.blockedMs"));
        blockedCol.setPrefWidth(120);
        blockedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().blockedDurationMillis())));

        view.table().getColumns().setAll(List.of(nameCol, samplesCol, blockedCol));
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
