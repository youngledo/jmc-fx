package io.github.youngledo.jmcfx.ui.socketio;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
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

/// Controller for the JFR Socket I/O tabbed data page.
public final class SocketIoPageController {

    private final SocketIoPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private StringBinding recordingContextBinding;
    private SocketIOViewModel viewModel;

    public SocketIoPageController(SocketIoPageView view, I18n i18n) {
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

    public void bind(SocketIOViewModel nextViewModel) {
        SocketIOViewModel currentViewModel = viewModel;
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
        view.titleLabel().textProperty().bind(i18n.text("socketio.title"));
        view.groupByHostAndPortButton().textProperty().bind(i18n.text("socketio.grouping.byHostAndPort"));
        view.groupByHostButton().textProperty().bind(i18n.text("socketio.grouping.byHost"));
        view.groupByPortButton().textProperty().bind(i18n.text("socketio.grouping.byPort"));
        view.timelineTab().textProperty().bind(i18n.text("socketio.tab.timeline"));
        view.durationTab().textProperty().bind(i18n.text("socketio.tab.duration"));
        view.eventLogTab().textProperty().bind(i18n.text("socketio.tab.eventLog"));
    }

    private void configureTables() {
        view.histogramTable().setPlaceholder(localizedTablePlaceholder("socketio.empty"));

        TableColumn<SocketIOHistogram, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("socketio.column.key"));
        keyCol.setPrefWidth(420);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<SocketIOHistogram, Number> readCountCol = new TableColumn<>();
        readCountCol.textProperty().bind(i18n.text("socketio.column.readCount"));
        readCountCol.setPrefWidth(80);
        readCountCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readCount()));
        useFormattedIntegerCells(readCountCol);

        TableColumn<SocketIOHistogram, Number> writeCountCol = new TableColumn<>();
        writeCountCol.textProperty().bind(i18n.text("socketio.column.writeCount"));
        writeCountCol.setPrefWidth(80);
        writeCountCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeCount()));
        useFormattedIntegerCells(writeCountCol);

        TableColumn<SocketIOHistogram, String> readSizeCol = new TableColumn<>();
        readSizeCol.textProperty().bind(i18n.text("socketio.column.readSize"));
        readSizeCol.setPrefWidth(100);
        readSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().readSize())));

        TableColumn<SocketIOHistogram, String> writeSizeCol = new TableColumn<>();
        writeSizeCol.textProperty().bind(i18n.text("socketio.column.writeSize"));
        writeSizeCol.setPrefWidth(100);
        writeSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().writeSize())));

        TableColumn<SocketIOHistogram, String> avgDurationCol = new TableColumn<>();
        avgDurationCol.textProperty().bind(i18n.text("socketio.column.avgDuration"));
        avgDurationCol.setPrefWidth(100);
        avgDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().avgDuration())));

        view.histogramTable().getColumns().setAll(List.of(
                keyCol, readCountCol, writeCountCol, readSizeCol, writeSizeCol, avgDurationCol));

        view.groupByHostAndPortButton().setOnAction(event -> setGrouping(SocketIOGrouping.BY_HOST_AND_PORT));
        view.groupByHostButton().setOnAction(event -> setGrouping(SocketIOGrouping.BY_HOST));
        view.groupByPortButton().setOnAction(event -> setGrouping(SocketIOGrouping.BY_PORT));

        view.eventTable().setPlaceholder(localizedTablePlaceholder("socketio.events.empty"));

        TableColumn<SocketIOEvent, String> eventTypeCol = new TableColumn<>();
        eventTypeCol.textProperty().bind(i18n.text("socketio.events.column.eventType"));
        eventTypeCol.setPrefWidth(140);
        eventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventType()));

        TableColumn<SocketIOEvent, String> hostCol = new TableColumn<>();
        hostCol.textProperty().bind(i18n.text("socketio.events.column.host"));
        hostCol.setPrefWidth(280);
        hostCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().host()));

        TableColumn<SocketIOEvent, Number> portCol = new TableColumn<>();
        portCol.textProperty().bind(i18n.text("socketio.events.column.port"));
        portCol.setPrefWidth(80);
        portCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().port()));
        useFormattedIntegerCells(portCol);

        TableColumn<SocketIOEvent, String> bytesCol = new TableColumn<>();
        bytesCol.textProperty().bind(i18n.text("socketio.events.column.bytes"));
        bytesCol.setPrefWidth(100);
        bytesCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().bytes())));

        TableColumn<SocketIOEvent, String> durationCol = new TableColumn<>();
        durationCol.textProperty().bind(i18n.text("socketio.events.column.duration"));
        durationCol.setPrefWidth(100);
        durationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().durationMillis())));

        TableColumn<SocketIOEvent, String> threadCol = new TableColumn<>();
        threadCol.textProperty().bind(i18n.text("socketio.events.column.thread"));
        threadCol.setPrefWidth(260);
        threadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        view.eventTable().getColumns().setAll(List.of(eventTypeCol, hostCol, portCol,
                bytesCol, durationCol, threadCol));
    }

    private void setGrouping(SocketIOGrouping grouping) {
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

    private String recordingContext(RecordingSummary recording) {
        if (recording == null) {
            return "";
        }
        ZoneId zone = ZoneId.systemDefault();
        String start = DisplayFormats.formatTimestamp(recording.startTime(), zone);
        String end = DisplayFormats.formatTimestamp(recording.endTime(), zone);
        String duration = DisplayFormats.formatDuration(recording.durationMillis());
        return i18n.format("socketio.recordingContext", start, end, duration);
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
