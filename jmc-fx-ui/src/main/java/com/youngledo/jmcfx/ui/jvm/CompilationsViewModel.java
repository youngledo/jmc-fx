package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.CompilationEvent;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Compilations page.
///
/// Loads compilation events, failures, and duration chart.
public class CompilationsViewModel {

    private final JvmInternalsService service;
    private final ObservableList<CompilationEvent> compilations = FXCollections.observableArrayList();
    private final ObservableList<CompilationEvent> failures = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> durationChart = new SimpleObjectProperty<>();

    public CompilationsViewModel(JvmInternalsService service) {
        this.service = service;
    }

    public ObservableList<CompilationEvent> compilations() {
        return compilations;
    }

    public ObservableList<CompilationEvent> failures() {
        return failures;
    }

    public ObjectProperty<ChartDefinition> durationChartProperty() {
        return durationChart;
    }

    public void load(RecordingSummary recording) {
        List<CompilationEvent> events = service.loadCompilationEvents(recording);
        List<CompilationEvent> failed = service.loadCompilationFailures(recording);
        ChartDefinition chart = service.loadCompilationDurationChart(recording);
        FxDispatch.run(() -> {
            compilations.setAll(events);
            failures.setAll(failed);
            durationChart.set(chart);
        });
    }
}
