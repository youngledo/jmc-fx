package io.github.youngledo.jmcfx.ui.recording;

import java.util.Objects;

import io.github.youngledo.jmcfx.ui.i18n.I18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;

/// Binds a clear command button to a recording workspace time range.
public final class RecordingTimeRangeClearButtonBinding implements AutoCloseable {

    private final Button button;
    private final ObjectProperty<RecordingTimeRange> timeRange;
    private final ChangeListener<RecordingTimeRange> timeRangeListener;

    public RecordingTimeRangeClearButtonBinding(
            Button button,
            I18n i18n,
            ObjectProperty<RecordingTimeRange> timeRange) {
        this.button = Objects.requireNonNull(button, "button");
        Objects.requireNonNull(i18n, "i18n");
        this.timeRange = timeRange;
        timeRangeListener = (observable, oldValue, newValue) -> refresh();
        button.textProperty().bind(i18n.text("recordingTimeRange.clear"));
        button.setOnAction(event -> clear());
        if (timeRange != null) {
            timeRange.addListener(timeRangeListener);
        }
        refresh();
    }

    @Override
    public void close() {
        button.textProperty().unbind();
        button.setOnAction(null);
        if (timeRange != null) {
            timeRange.removeListener(timeRangeListener);
        }
        setVisible(false);
    }

    private void clear() {
        if (timeRange != null) {
            timeRange.set(null);
        }
        refresh();
    }

    private void refresh() {
        setVisible(timeRange != null && timeRange.get() != null);
    }

    private void setVisible(boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
