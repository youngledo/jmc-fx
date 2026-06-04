package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.GcHeapSummary;
import com.youngledo.jmcfx.domain.model.GcReferenceStat;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the GC Details page.
///
/// Loads GC events, reference stats, heap summaries, and chart definitions.
public class GcDetailsViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<GcEvent> gcEvents = FXCollections.observableArrayList();
    private final ObservableList<GcReferenceStat> referenceStats = FXCollections.observableArrayList();
    private final ObservableList<GcHeapSummary> heapSummaries = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> heapChart = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> metaspaceChart = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> pauseChart = new SimpleObjectProperty<>();

    public GcDetailsViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
    }

    public ObservableList<GcEvent> gcEvents() {
        return gcEvents;
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

    public void load(RecordingSummary recording) {
        List<GcEvent> events = service.loadGcEvents(recording);
        List<GcReferenceStat> refs = service.loadGcReferenceStats(recording);
        List<GcHeapSummary> heaps = service.loadGcHeapSummaries(recording);
        ChartDefinition heap = service.loadGcHeapChart(recording);
        ChartDefinition meta = service.loadGcMetaspaceChart(recording);
        ChartDefinition pause = service.loadGcPauseChart(recording);
        FxDispatch.run(() -> {
            gcEvents.setAll(events);
            referenceStats.setAll(refs);
            heapSummaries.setAll(heaps);
            heapChart.set(heap);
            metaspaceChart.set(meta);
            pauseChart.set(pause);
        });
    }
}
