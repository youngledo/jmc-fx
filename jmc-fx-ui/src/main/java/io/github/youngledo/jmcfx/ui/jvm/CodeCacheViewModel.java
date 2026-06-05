package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.CodeCacheStats;
import io.github.youngledo.jmcfx.domain.model.CodeCacheSweep;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeFilters;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Code Cache page.
///
/// Loads code cache sweeps, statistics, and chart definitions.
public class CodeCacheViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<CodeCacheSweep> allSweeps = FXCollections.observableArrayList();
    private final ObservableList<CodeCacheSweep> sweeps = FXCollections.observableArrayList();
    private final ObservableList<CodeCacheStats> statistics = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingTimeRange> timeRange = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> entriesChart = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> sweepChart = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public CodeCacheViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
        timeRange.addListener((observable, oldValue, newValue) -> applySweepFilter());
    }

    public ObservableList<CodeCacheSweep> sweeps() {
        return sweeps;
    }

    public ObjectProperty<RecordingTimeRange> timeRangeProperty() {
        return timeRange;
    }

    public ObservableList<CodeCacheStats> statistics() {
        return statistics;
    }

    public ObjectProperty<ChartDefinition> entriesChartProperty() {
        return entriesChart;
    }

    public ObjectProperty<ChartDefinition> sweepChartProperty() {
        return sweepChart;
    }

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public void load(RecordingSummary recording) {
        List<CodeCacheSweep> sweepList = service.loadCodeCacheSweeps(recording);
        List<CodeCacheStats> statsList = service.loadCodeCacheStatistics(recording);
        ChartDefinition entries = service.loadCodeCacheEntriesChart(recording);
        ChartDefinition sweep = service.loadCodeCacheSweepChart(recording);
        FxDispatch.run(() -> {
            currentRecording.set(recording);
            allSweeps.setAll(sweepList);
            applySweepFilter();
            statistics.setAll(statsList);
            entriesChart.set(entries);
            sweepChart.set(sweep);
        });
    }

    private void applySweepFilter() {
        RecordingTimeRangeFilters.apply(sweeps, allSweeps, timeRange.get(), CodeCacheSweep::startTime);
    }
}
