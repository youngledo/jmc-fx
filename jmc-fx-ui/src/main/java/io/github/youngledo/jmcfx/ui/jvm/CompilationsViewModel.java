package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.CompilationEvent;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeFilters;
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
    private final ObservableList<CompilationEvent> allCompilations = FXCollections.observableArrayList();
    private final ObservableList<CompilationEvent> compilations = FXCollections.observableArrayList();
    private final ObservableList<CompilationEvent> allFailures = FXCollections.observableArrayList();
    private final ObservableList<CompilationEvent> failures = FXCollections.observableArrayList();
    private final ObjectProperty<RecordingTimeRange> timeRange = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> durationChart = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public CompilationsViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
        timeRange.addListener((observable, oldValue, newValue) -> applyEventFilter());
    }

    public ObservableList<CompilationEvent> compilations() {
        return compilations;
    }

    public ObservableList<CompilationEvent> failures() {
        return failures;
    }

    public ObjectProperty<RecordingTimeRange> timeRangeProperty() {
        return timeRange;
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
            allCompilations.setAll(events);
            allFailures.setAll(failed);
            applyEventFilter();
            durationChart.set(chart);
        });
    }

    private void applyEventFilter() {
        RecordingTimeRangeFilters.apply(compilations, allCompilations, timeRange.get(), CompilationEvent::startTime);
        RecordingTimeRangeFilters.apply(failures, allFailures, timeRange.get(), CompilationEvent::startTime);
    }
}
