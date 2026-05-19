package com.youngledo.jmcfx.ui.recording;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.RecordingRepository;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for opened JFR recordings.
///
/// Recording loading is delegated to the `RecordingRepository` port so the UI
/// layer never depends on OpenJDK JMC APIs directly.
public class RecordingBrowserViewModel {

    private final RecordingRepository recordingRepository;
    private final ObservableList<RecordingSummary> recordings = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingSummary> selectedRecording = new SimpleObjectProperty<>();

    public RecordingBrowserViewModel(RecordingRepository recordingRepository) {
        this.recordingRepository = recordingRepository;
    }

    public ObservableList<RecordingSummary> recordingsProperty() {
        return recordings;
    }

    public ObjectProperty<RecordingSummary> selectedRecordingProperty() {
        return selectedRecording;
    }

    public void openRecording(Path path) {
        RecordingSummary summary = recordingRepository.open(path);
        recordings.add(summary);
        selectedRecording.set(summary);
    }
}
