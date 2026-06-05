package io.github.youngledo.jmcfx.ui.socketio;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.application.LoadSocketIOUseCase;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Socket I/O page.
///
/// Manages socket I/O histogram with configurable grouping, event log, and timeline chart.
public class SocketIOViewModel {

    private final LoadSocketIOUseCase socketIOService;
    private final ObservableList<SocketIOHistogram> histogram = FXCollections.observableArrayList();
    private final ObservableList<SocketIOEvent> allEvents = FXCollections.observableArrayList();
    private final ObservableList<SocketIOEvent> events = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingTimeRange> timeRange = new SimpleObjectProperty<>();
    private final ObjectProperty<SocketIOGrouping> grouping =
            new SimpleObjectProperty<>(SocketIOGrouping.BY_HOST_AND_PORT);
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public SocketIOViewModel(LoadSocketIOUseCase socketIOService) {
        this.socketIOService = socketIOService;
        timeRange.addListener((observable, oldValue, newValue) -> applyEventFilter());
    }

    public ObservableList<SocketIOHistogram> histogramProperty() {
        return histogram;
    }

    public ObservableList<SocketIOEvent> eventsProperty() {
        return events;
    }

    public ObjectProperty<RecordingTimeRange> timeRangeProperty() {
        return timeRange;
    }

    public ObjectProperty<SocketIOGrouping> groupingProperty() {
        return grouping;
    }

    public ObjectProperty<ChartDefinition> timelineProperty() {
        return timeline;
    }

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public void load(RecordingSummary recording) {
        List<SocketIOHistogram> histogramData =
                socketIOService.loadSocketIOHistogram(recording, grouping.get());
        List<SocketIOEvent> eventData = socketIOService.loadSocketIOEvents(recording);
        ChartDefinition chart = socketIOService.loadTimeline(recording);
        FxDispatch.run(() -> {
            currentRecording.set(recording);
            histogram.setAll(histogramData);
            allEvents.setAll(eventData);
            applyEventFilter();
            timeline.set(chart);
        });
    }

    public void setGrouping(SocketIOGrouping newGrouping) {
        grouping.set(newGrouping);
        if (currentRecording.get() != null) {
            reloadHistogram();
        }
    }

    private void reloadHistogram() {
        List<SocketIOHistogram> data =
                socketIOService.loadSocketIOHistogram(currentRecording.get(), grouping.get());
        FxDispatch.run(() -> histogram.setAll(data));
    }

    private void reloadEvents() {
        List<SocketIOEvent> data = socketIOService.loadSocketIOEvents(currentRecording.get());
        FxDispatch.run(() -> {
            allEvents.setAll(data);
            applyEventFilter();
        });
    }

    private void applyEventFilter() {
        RecordingTimeRange range = timeRange.get();
        if (range == null) {
            events.setAll(allEvents);
            return;
        }
        events.setAll(allEvents.stream()
                .filter(event -> range.contains(event.timestamp()))
                .toList());
    }
}
