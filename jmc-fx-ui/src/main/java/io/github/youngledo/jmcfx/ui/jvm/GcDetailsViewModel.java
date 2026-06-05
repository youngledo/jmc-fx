package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.GcHeapSummary;
import io.github.youngledo.jmcfx.domain.model.GcReferenceStat;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeFilters;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the GC Details page.
///
/// Loads GC events, reference stats, heap summaries, and chart definitions.
public class GcDetailsViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<GcEvent> allGcEvents = FXCollections.observableArrayList();
    private final ObservableList<GcEvent> gcEvents = FXCollections.observableArrayList();
    private final ObservableList<GcReferenceStat> referenceStats = FXCollections.observableArrayList();
    private final ObservableList<GcHeapSummary> heapSummaries = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingTimeRange> timeRange = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> heapChart = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> metaspaceChart = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> pauseChart = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public GcDetailsViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
        timeRange.addListener((observable, oldValue, newValue) -> applyEventFilter());
    }

    public ObservableList<GcEvent> gcEvents() {
        return gcEvents;
    }

    public ObjectProperty<RecordingTimeRange> timeRangeProperty() {
        return timeRange;
    }

    public ObservableList<GcReferenceStat> referenceStats() {
        return referenceStats;
    }

    public ObservableList<GcHeapSummary> heapSummaries() {
        return heapSummaries;
    }

    public ObjectProperty<ChartDefinition> heapChartProperty() {
        return heapChart;
    }

    public ObjectProperty<ChartDefinition> metaspaceChartProperty() {
        return metaspaceChart;
    }

    public ObjectProperty<ChartDefinition> pauseChartProperty() {
        return pauseChart;
    }

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public void load(RecordingSummary recording) {
        List<GcEvent> events = service.loadGcEvents(recording);
        List<GcReferenceStat> refs = service.loadGcReferenceStats(recording);
        List<GcHeapSummary> heaps = service.loadGcHeapSummaries(recording);
        ChartDefinition heap = service.loadGcHeapChart(recording);
        ChartDefinition meta = service.loadGcMetaspaceChart(recording);
        ChartDefinition pause = service.loadGcPauseChart(recording);
        FxDispatch.run(() -> {
            currentRecording.set(recording);
            allGcEvents.setAll(events);
            applyEventFilter();
            referenceStats.setAll(refs);
            heapSummaries.setAll(heaps);
            heapChart.set(heap);
            metaspaceChart.set(meta);
            pauseChart.set(pause);
        });
    }

    private void applyEventFilter() {
        RecordingTimeRangeFilters.apply(gcEvents, allGcEvents, timeRange.get(), GcEvent::startTime);
    }
}
