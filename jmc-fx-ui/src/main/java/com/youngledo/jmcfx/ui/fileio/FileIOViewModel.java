package com.youngledo.jmcfx.ui.fileio;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.FileIOService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the File I/O page.
///
/// Manages file I/O histogram, event log, and timeline chart.
public class FileIOViewModel {

    private final FileIOService fileIOService;
    private final ObservableList<FileIOHistogram> histogram = FXCollections.observableArrayList();
    private final ObservableList<FileIOEvent> events = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private RecordingSummary currentRecording;

    public FileIOViewModel(FileIOService fileIOService) {
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
        reloadHistogram();
        reloadEvents();
        timeline.set(fileIOService.loadTimeline(recording));
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
