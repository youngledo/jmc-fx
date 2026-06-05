package io.github.youngledo.jmcfx.ui.metadata;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

/// Controller for the JFR Metadata split table/detail page.
public final class MetadataPageController {

    private final MetadataPageView view;
    private final I18n i18n;
    private final ChangeListener<JfrMetadataEventType> selectedEventTypeListener;
    private JfrMetadataViewModel viewModel;

    public MetadataPageController(MetadataPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        this.selectedEventTypeListener =
                (observable, oldValue, newValue) -> view.eventTypesTable().getSelectionModel().select(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureMetadataTable();
        bind(null);
    }

    public void bind(JfrMetadataViewModel nextViewModel) {
        if (viewModel != null) {
            viewModel.selectedEventTypeProperty().removeListener(selectedEventTypeListener);
        }
        view.summaryLabel().textProperty().unbind();
        view.detailArea().textProperty().unbind();
        view.eventTypesTable().setItems(FXCollections.emptyObservableList());
        view.eventTypesTable().getSelectionModel().clearSelection();
        view.summaryLabel().setText(i18n.get("metadata.summary"));
        view.detailArea().setText("");
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.summaryLabel().textProperty().bind(nextViewModel.summaryProperty());
        view.detailArea().textProperty().bind(nextViewModel.selectedDetailProperty());
        view.eventTypesTable().setItems(nextViewModel.eventTypesProperty());
        view.eventTypesTable().getSelectionModel().select(nextViewModel.selectedEventTypeProperty().get());
        nextViewModel.selectedEventTypeProperty().addListener(selectedEventTypeListener);
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("metadata.title"));
        view.detailTitleLabel().textProperty().bind(i18n.text("metadata.detail.title"));
    }

    private void configureMetadataTable() {
        view.eventTypesTable().setPlaceholder(localizedTablePlaceholder("metadata.empty"));

        TableColumn<JfrMetadataEventType, String> categoryCol = localizedColumn("metadata.column.category");
        categoryCol.setPrefWidth(220);
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category()));

        TableColumn<JfrMetadataEventType, String> nameCol = localizedColumn("metadata.column.name");
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<JfrMetadataEventType, String> idCol = localizedColumn("metadata.column.id");
        idCol.setPrefWidth(280);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));

        TableColumn<JfrMetadataEventType, String> eventCountLabelCol =
                localizedColumn("metadata.column.eventCount");
        TableColumn<JfrMetadataEventType, Number> eventCountCol = new TableColumn<>();
        eventCountCol.textProperty().bind(eventCountLabelCol.textProperty());
        eventCountCol.setPrefWidth(100);
        eventCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().eventCount()));
        useFormattedIntegerCells(eventCountCol);

        TableColumn<JfrMetadataEventType, String> fieldCountLabelCol =
                localizedColumn("metadata.column.fieldCount");
        TableColumn<JfrMetadataEventType, Number> fieldCountCol = new TableColumn<>();
        fieldCountCol.textProperty().bind(fieldCountLabelCol.textProperty());
        fieldCountCol.setPrefWidth(90);
        fieldCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(cell.getValue().fieldCount()));
        useFormattedIntegerCells(fieldCountCol);

        view.eventTypesTable().getColumns().setAll(List.of(categoryCol, nameCol, idCol,
                eventCountCol, fieldCountCol));
        view.eventTypesTable().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, eventType) -> {
                    if (viewModel != null) {
                        viewModel.selectedEventTypeProperty().set(eventType);
                    }
                });
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
