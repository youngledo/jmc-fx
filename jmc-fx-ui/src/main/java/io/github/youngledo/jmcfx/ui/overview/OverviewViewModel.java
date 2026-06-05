package io.github.youngledo.jmcfx.ui.overview;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/// View model for the v1 Overview dashboard.
///
/// The overview combines real recording metadata with honest unavailable states
/// for workflows whose adapters are not connected yet.
public class OverviewViewModel {

    private final StringProperty recordingName = new SimpleStringProperty("");
    private final StringProperty recordingDetails = new SimpleStringProperty("");
    private final StringProperty analysisStatus = new SimpleStringProperty("");
    private final StringProperty jvmStatus = new SimpleStringProperty("");
    private final ObjectProperty<RecordingSummary> recording = new SimpleObjectProperty<>();

    public StringProperty recordingNameProperty() {
        return recordingName;
    }

    public StringProperty recordingDetailsProperty() {
        return recordingDetails;
    }

    public StringProperty analysisStatusProperty() {
        return analysisStatus;
    }

    public StringProperty jvmStatusProperty() {
        return jvmStatus;
    }

    public ObjectProperty<RecordingSummary> recordingProperty() {
        return recording;
    }

    public void showRecording(RecordingSummary rec, String formattedDetails) {
        recording.set(rec);
        recordingName.set(rec.name());
        recordingDetails.set(formattedDetails);
    }
}
