package io.github.youngledo.jmcfx.ui.socketio;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.application.LoadSocketIOUseCase;
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
    private final ObservableList<SocketIOEvent> events = FXCollections.observableArrayList();
    private final ObjectProperty<SocketIOGrouping> grouping =
            new SimpleObjectProperty<>(SocketIOGrouping.BY_HOST_AND_PORT);
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private RecordingSummary currentRecording;

    public SocketIOViewModel(LoadSocketIOUseCase socketIOService) {
        this.socketIOService = socketIOService;
    }

    public ObservableList<SocketIOHistogram> histogramProperty() {
        return histogram;
    }

    public ObservableList<SocketIOEvent> eventsProperty() {
        return events;
    }

    public ObjectProperty<SocketIOGrouping> groupingProperty() {
        return grouping;
    }

    public ObjectProperty<ChartDefinition> timelineProperty() {
        return timeline;
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<SocketIOHistogram> histogramData =
                socketIOService.loadSocketIOHistogram(currentRecording, grouping.get());
        List<SocketIOEvent> eventData = socketIOService.loadSocketIOEvents(currentRecording);
        ChartDefinition chart = socketIOService.loadTimeline(recording);
        FxDispatch.run(() -> {
            histogram.setAll(histogramData);
            events.setAll(eventData);
            timeline.set(chart);
        });
    }

    public void setGrouping(SocketIOGrouping newGrouping) {
        grouping.set(newGrouping);
        if (currentRecording != null) {
            reloadHistogram();
        }
    }

    private void reloadHistogram() {
        List<SocketIOHistogram> data =
                socketIOService.loadSocketIOHistogram(currentRecording, grouping.get());
        FxDispatch.run(() -> histogram.setAll(data));
    }

    private void reloadEvents() {
        List<SocketIOEvent> data = socketIOService.loadSocketIOEvents(currentRecording);
        FxDispatch.run(() -> events.setAll(data));
    }
}
