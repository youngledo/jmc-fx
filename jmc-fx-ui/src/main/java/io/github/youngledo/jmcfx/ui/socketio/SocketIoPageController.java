package io.github.youngledo.jmcfx.ui.socketio;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeChartBinding;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
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
    private final ChangeListener<RecordingTimeRange> sharedTimeRangeListener;
    private StringBinding recordingContextBinding;
    private SocketIOViewModel viewModel;
    private ObjectProperty<RecordingTimeRange> sharedTimeRange;
    private RecordingTimeRangeChartBinding timelineSelectionBinding;

    public SocketIoPageController(SocketIoPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> setTimelineData(newValue);
        sharedTimeRangeListener = (observable, oldValue, newValue) -> {
            refreshTimeRangeStatus();
        };
    }

    public void configure() {
        bindLocalizedText();
        configureTables();
        bind(null, null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.histogramTable(), view.eventTable());
    }

    public void bind(SocketIOViewModel nextViewModel) {
        bind(nextViewModel, null);
    }

    public void bind(SocketIOViewModel nextViewModel, ObjectProperty<RecordingTimeRange> nextSharedTimeRange) {
        SocketIOViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.timelineProperty().removeListener(timelineListener);
            currentViewModel.timeRangeProperty().unbind();
            currentViewModel.timeRangeProperty().set(null);
        }
        if (sharedTimeRange != null) {
            sharedTimeRange.removeListener(sharedTimeRangeListener);
        }
        if (timelineSelectionBinding != null) {
            timelineSelectionBinding.close();
            timelineSelectionBinding = null;
        }
        if (recordingContextBinding != null) {
            view.recordingContextLabel().textProperty().unbind();
            recordingContextBinding.dispose();
            recordingContextBinding = null;
        }
        view.recordingContextLabel().setText("");
        view.clearTimeRangeButton().setVisible(false);
        view.clearTimeRangeButton().setManaged(false);
        view.timelineChart().setData(null);
        view.histogramTable().setItems(FXCollections.emptyObservableList());
        view.eventTable().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        sharedTimeRange = nextSharedTimeRange;
        if (nextViewModel == null) {
            return;
        }
        view.histogramTable().setItems(nextViewModel.histogramProperty());
        view.eventTable().setItems(nextViewModel.eventsProperty());
        nextViewModel.timelineProperty().addListener(timelineListener);
        timelineSelectionBinding = new RecordingTimeRangeChartBinding(view.timelineChart(), nextSharedTimeRange);
        setTimelineData(nextViewModel.timelineProperty().get());
        if (nextSharedTimeRange != null) {
            nextViewModel.timeRangeProperty().bind(nextSharedTimeRange);
            nextSharedTimeRange.addListener(sharedTimeRangeListener);
        } else {
            nextViewModel.timeRangeProperty().set(null);
        }
        recordingContextBinding = Bindings.createStringBinding(
                () -> recordingContext(nextViewModel.currentRecordingProperty().get(),
                        nextSharedTimeRange == null ? null : nextSharedTimeRange.get()),
                nextViewModel.currentRecordingProperty(),
                nextSharedTimeRange == null ? nextViewModel.currentRecordingProperty() : nextSharedTimeRange,
                i18n.localeProperty());
        view.recordingContextLabel().textProperty().bind(recordingContextBinding);
        refreshTimeRangeStatus();
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("socketio.title"));
        view.clearTimeRangeButton().textProperty().bind(i18n.text("recordingTimeRange.clear"));
        view.groupByHostAndPortButton().textProperty().bind(i18n.text("socketio.grouping.byHostAndPort"));
        view.groupByHostButton().textProperty().bind(i18n.text("socketio.grouping.byHost"));
        view.groupByPortButton().textProperty().bind(i18n.text("socketio.grouping.byPort"));
        view.timelineTab().textProperty().bind(i18n.text("socketio.tab.timeline"));
        view.durationTab().textProperty().bind(i18n.text("socketio.tab.duration"));
        view.eventLogTab().textProperty().bind(i18n.text("socketio.tab.eventLog"));
        view.clearTimeRangeButton().setOnAction(event -> clearSharedTimeRange());
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

    private void setTimelineData(ChartDefinition definition) {
        if (timelineSelectionBinding != null) {
            timelineSelectionBinding.setData(definition);
            return;
        }
        view.timelineChart().setData(definition);
    }

    private void clearSharedTimeRange() {
        if (sharedTimeRange != null) {
            sharedTimeRange.set(null);
        } else if (viewModel != null) {
            viewModel.timeRangeProperty().set(null);
        }
        refreshTimeRangeStatus();
    }

    private void refreshTimeRangeStatus() {
        RecordingTimeRange range = sharedTimeRange == null ? null : sharedTimeRange.get();
        boolean active = range != null;
        view.clearTimeRangeButton().setVisible(active);
        view.clearTimeRangeButton().setManaged(active);
    }

    private String recordingContext(RecordingSummary recording, RecordingTimeRange range) {
        if (recording == null) {
            return "";
        }
        ZoneId zone = ZoneId.systemDefault();
        if (range != null) {
            String start = DisplayFormats.formatTimestamp(
                    java.time.Instant.ofEpochMilli(range.startEpochMillis()), zone);
            String end = DisplayFormats.formatTimestamp(
                    java.time.Instant.ofEpochMilli(range.endEpochMillis()), zone);
            String duration = DisplayFormats.formatDuration(range.durationMillis());
            return i18n.format("recordingTimeRange.active", start, end, duration);
        }
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
