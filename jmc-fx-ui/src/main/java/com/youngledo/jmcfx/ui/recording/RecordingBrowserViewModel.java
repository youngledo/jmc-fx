package com.youngledo.jmcfx.ui.recording;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.application.OpenRecordingUseCase;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for opened JFR recordings.
///
/// Recording loading is delegated to the `OpenRecordingUseCase` port so the UI
/// layer never depends on OpenJDK JMC APIs directly.
public class RecordingBrowserViewModel {

    private final OpenRecordingUseCase openRecording;
    private final ObservableList<RecordingSummary> recordings = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingSummary> selectedRecording = new SimpleObjectProperty<>();

    public RecordingBrowserViewModel(OpenRecordingUseCase openRecording) {
        this.openRecording = openRecording;
    }

    public ObservableList<RecordingSummary> recordingsProperty() {
        return recordings;
    }

    public ObjectProperty<RecordingSummary> selectedRecordingProperty() {
        return selectedRecording;
    }

    public void openRecording(Path path) {
        RecordingSummary summary = openRecording.open(path);
        recordings.add(summary);
        selectedRecording.set(summary);
    }
}
