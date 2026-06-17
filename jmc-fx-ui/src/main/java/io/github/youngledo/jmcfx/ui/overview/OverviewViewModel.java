package io.github.youngledo.jmcfx.ui.overview;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.jvms.LiveFlightRecordingOrigin;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
    private final StringProperty liveOriginDetails = new SimpleStringProperty("");
    private final StringProperty analysisStatus = new SimpleStringProperty("");
    private final StringProperty jvmStatus = new SimpleStringProperty("");
    private final ObjectProperty<RecordingSummary> recording = new SimpleObjectProperty<>();
    private final ObjectProperty<LiveFlightRecordingOrigin> liveOrigin = new SimpleObjectProperty<>();
    private final BooleanProperty liveOriginVisible = new SimpleBooleanProperty(false);

    public StringProperty recordingNameProperty() {
        return recordingName;
    }

    public StringProperty recordingDetailsProperty() {
        return recordingDetails;
    }

    public StringProperty liveOriginDetailsProperty() {
        return liveOriginDetails;
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

    public ObjectProperty<LiveFlightRecordingOrigin> liveOriginProperty() {
        return liveOrigin;
    }

    public BooleanProperty liveOriginVisibleProperty() {
        return liveOriginVisible;
    }

    public void showRecording(RecordingSummary rec, String formattedDetails) {
        showRecording(rec, formattedDetails, null, "");
    }

    public void showRecording(RecordingSummary rec, String formattedDetails,
            LiveFlightRecordingOrigin origin, String formattedLiveOrigin) {
        recording.set(rec);
        recordingName.set(rec.name());
        recordingDetails.set(formattedDetails);
        liveOrigin.set(origin);
        liveOriginDetails.set(origin == null ? "" : formattedLiveOrigin);
        liveOriginVisible.set(origin != null);
    }
}
