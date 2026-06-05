package io.github.youngledo.jmcfx.ui.fileio;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR File I/O tabbed data page.
public final class FileIoPageController {

    private final FileIoPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private StringBinding recordingContextBinding;
    private FileIOViewModel viewModel;

    public FileIoPageController(FileIoPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> view.timelineChart().setData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTables();
        bind(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.histogramTable(), view.eventTable());
    }

    public void bind(FileIOViewModel nextViewModel) {
        FileIOViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.timelineProperty().removeListener(timelineListener);
        }
        if (recordingContextBinding != null) {
            view.recordingContextLabel().textProperty().unbind();
            recordingContextBinding.dispose();
            recordingContextBinding = null;
        }
        view.recordingContextLabel().setText("");
        view.timelineChart().setData(null);
        view.histogramTable().setItems(FXCollections.emptyObservableList());
        view.eventTable().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.histogramTable().setItems(nextViewModel.histogramProperty());
        view.eventTable().setItems(nextViewModel.eventsProperty());
        nextViewModel.timelineProperty().addListener(timelineListener);
        view.timelineChart().setData(nextViewModel.timelineProperty().get());
        recordingContextBinding = Bindings.createStringBinding(
                () -> recordingContext(nextViewModel.currentRecordingProperty().get()),
                nextViewModel.currentRecordingProperty(),
                i18n.localeProperty());
        view.recordingContextLabel().textProperty().bind(recordingContextBinding);
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("fileio.title"));
        view.timelineTab().textProperty().bind(i18n.text("fileio.tab.timeline"));
        view.durationTab().textProperty().bind(i18n.text("fileio.tab.duration"));
        view.eventLogTab().textProperty().bind(i18n.text("fileio.tab.eventLog"));
    }

    private void configureTables() {
        view.histogramTable().setPlaceholder(localizedTablePlaceholder("fileio.empty"));

        TableColumn<FileIOHistogram, String> pathCol = new TableColumn<>();
        pathCol.textProperty().bind(i18n.text("fileio.column.path"));
        pathCol.setPrefWidth(560);
        pathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().path()));

        TableColumn<FileIOHistogram, Number> readCountCol = new TableColumn<>();
        readCountCol.textProperty().bind(i18n.text("fileio.column.readCount"));
        readCountCol.setPrefWidth(80);
        readCountCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readCount()));
        useFormattedIntegerCells(readCountCol);

        TableColumn<FileIOHistogram, Number> writeCountCol = new TableColumn<>();
        writeCountCol.textProperty().bind(i18n.text("fileio.column.writeCount"));
        writeCountCol.setPrefWidth(80);
        writeCountCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeCount()));
        useFormattedIntegerCells(writeCountCol);

        TableColumn<FileIOHistogram, String> readSizeCol = new TableColumn<>();
        readSizeCol.textProperty().bind(i18n.text("fileio.column.readSize"));
        readSizeCol.setPrefWidth(100);
        readSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().readSize())));

        TableColumn<FileIOHistogram, String> writeSizeCol = new TableColumn<>();
        writeSizeCol.textProperty().bind(i18n.text("fileio.column.writeSize"));
        writeSizeCol.setPrefWidth(100);
        writeSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().writeSize())));

        TableColumn<FileIOHistogram, String> avgDurationCol = new TableColumn<>();
        avgDurationCol.textProperty().bind(i18n.text("fileio.column.avgDuration"));
        avgDurationCol.setPrefWidth(100);
        avgDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().avgDuration())));

        view.histogramTable().getColumns().setAll(List.of(pathCol, readCountCol, writeCountCol,
                readSizeCol, writeSizeCol, avgDurationCol));

        view.eventTable().setPlaceholder(localizedTablePlaceholder("fileio.events.empty"));

        TableColumn<FileIOEvent, String> eventTypeCol = new TableColumn<>();
        eventTypeCol.textProperty().bind(i18n.text("fileio.events.column.eventType"));
        eventTypeCol.setPrefWidth(140);
        eventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventType()));

        TableColumn<FileIOEvent, String> eventPathCol = new TableColumn<>();
        eventPathCol.textProperty().bind(i18n.text("fileio.events.column.path"));
        eventPathCol.setPrefWidth(560);
        eventPathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().path()));

        TableColumn<FileIOEvent, String> eventBytesCol = new TableColumn<>();
        eventBytesCol.textProperty().bind(i18n.text("fileio.events.column.bytes"));
        eventBytesCol.setPrefWidth(100);
        eventBytesCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().bytes())));

        TableColumn<FileIOEvent, String> eventDurationCol = new TableColumn<>();
        eventDurationCol.textProperty().bind(i18n.text("fileio.events.column.duration"));
        eventDurationCol.setPrefWidth(100);
        eventDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().durationMillis())));

        TableColumn<FileIOEvent, String> eventThreadCol = new TableColumn<>();
        eventThreadCol.textProperty().bind(i18n.text("fileio.events.column.thread"));
        eventThreadCol.setPrefWidth(260);
        eventThreadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        view.eventTable().getColumns().setAll(List.of(eventTypeCol, eventPathCol, eventBytesCol,
                eventDurationCol, eventThreadCol));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private String recordingContext(RecordingSummary recording) {
        if (recording == null) {
            return "";
        }
        ZoneId zone = ZoneId.systemDefault();
        String start = DisplayFormats.formatTimestamp(recording.startTime(), zone);
        String end = DisplayFormats.formatTimestamp(recording.endTime(), zone);
        String duration = DisplayFormats.formatDuration(recording.durationMillis());
        return i18n.format("fileio.recordingContext", start, end, duration);
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
