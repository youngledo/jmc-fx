package io.github.youngledo.jmcfx.ui.fileio;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadFileIOUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the File I/O page.
///
/// Manages file I/O histogram, event log, and timeline chart.
public class FileIOViewModel {

    private final LoadFileIOUseCase fileIOService;
    private final ObservableList<FileIOHistogram> histogram = FXCollections.observableArrayList();
    private final ObservableList<FileIOEvent> events = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private RecordingSummary currentRecording;

    public FileIOViewModel(LoadFileIOUseCase fileIOService) {
        this.fileIOService = fileIOService;
    }

    public ObservableList<FileIOHistogram> histogramProperty() {
        return histogram;
    }

    public ObservableList<FileIOEvent> eventsProperty() {
        return events;
    }

    public ObjectProperty<ChartDefinition> timelineProperty() {
        return timeline;
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<FileIOHistogram> histogramData = fileIOService.loadFileIOHistogram(currentRecording);
        List<FileIOEvent> eventData = fileIOService.loadFileIOEvents(currentRecording);
        ChartDefinition chart = fileIOService.loadTimeline(recording);
        FxDispatch.run(() -> {
            histogram.setAll(histogramData);
            events.setAll(eventData);
            timeline.set(chart);
        });
    }

    private void reloadHistogram() {
        List<FileIOHistogram> data = fileIOService.loadFileIOHistogram(currentRecording);
        histogram.setAll(data);
    }

    private void reloadEvents() {
        List<FileIOEvent> data = fileIOService.loadFileIOEvents(currentRecording);
        events.setAll(data);
    }
}
