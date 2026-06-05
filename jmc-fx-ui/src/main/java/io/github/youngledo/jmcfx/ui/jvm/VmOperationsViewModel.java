package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.VmOperationEvent;
import io.github.youngledo.jmcfx.domain.model.VmOperationSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeFilters;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the VM Operations page.
///
/// Loads VM operation summary and event data from a recording.
public class VmOperationsViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<VmOperationSummary> summary = FXCollections.observableArrayList();
    private final ObservableList<VmOperationEvent> allEvents = FXCollections.observableArrayList();
    private final ObservableList<VmOperationEvent> events = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingTimeRange> timeRange = new SimpleObjectProperty<>();

    public VmOperationsViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
        timeRange.addListener((observable, oldValue, newValue) -> applyEventFilter());
    }

    public ObservableList<VmOperationSummary> summary() {
        return summary;
    }

    public ObservableList<VmOperationEvent> events() {
        return events;
    }

    public ObjectProperty<RecordingTimeRange> timeRangeProperty() {
        return timeRange;
    }

    public void load(RecordingSummary recording) {
        List<VmOperationSummary> summ = service.loadVmOperationSummary(recording);
        List<VmOperationEvent> evts = service.loadVmOperationEvents(recording);
        FxDispatch.run(() -> {
            summary.setAll(summ);
            allEvents.setAll(evts);
            applyEventFilter();
        });
    }

    private void applyEventFilter() {
        RecordingTimeRangeFilters.apply(events, allEvents, timeRange.get(), VmOperationEvent::startTime);
    }
}
