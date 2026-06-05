package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.CompilationEvent;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Compilations page.
///
/// Loads compilation events, failures, and duration chart.
public class CompilationsViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<CompilationEvent> compilations = FXCollections.observableArrayList();
    private final ObservableList<CompilationEvent> failures = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> durationChart = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public CompilationsViewModel(LoadJvmInternalsUseCase service) {
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

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public void load(RecordingSummary recording) {
        List<CompilationEvent> events = service.loadCompilationEvents(recording);
        List<CompilationEvent> failed = service.loadCompilationFailures(recording);
        ChartDefinition chart = service.loadCompilationDurationChart(recording);
        FxDispatch.run(() -> {
            currentRecording.set(recording);
            compilations.setAll(events);
            failures.setAll(failed);
            durationChart.set(chart);
        });
    }
}
