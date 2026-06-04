package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.CodeCacheStats;
import com.youngledo.jmcfx.domain.model.CodeCacheSweep;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Code Cache page.
///
/// Loads code cache sweeps, statistics, and chart definitions.
public class CodeCacheViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<CodeCacheSweep> sweeps = FXCollections.observableArrayList();
    private final ObservableList<CodeCacheStats> statistics = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> entriesChart = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> sweepChart = new SimpleObjectProperty<>();

    public CodeCacheViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
    }

    public ObservableList<CodeCacheSweep> sweeps() {
        return sweeps;
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

    public void load(RecordingSummary recording) {
        List<CodeCacheSweep> sweepList = service.loadCodeCacheSweeps(recording);
        List<CodeCacheStats> statsList = service.loadCodeCacheStatistics(recording);
        ChartDefinition entries = service.loadCodeCacheEntriesChart(recording);
        ChartDefinition sweep = service.loadCodeCacheSweepChart(recording);
        FxDispatch.run(() -> {
            sweeps.setAll(sweepList);
            statistics.setAll(statsList);
            entriesChart.set(entries);
            sweepChart.set(sweep);
        });
    }
}
