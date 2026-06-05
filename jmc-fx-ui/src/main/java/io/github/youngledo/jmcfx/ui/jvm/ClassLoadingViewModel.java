package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import io.github.youngledo.jmcfx.domain.model.ClassloaderSummary;
import io.github.youngledo.jmcfx.domain.model.ClassloadEvent;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Class Loading page.
///
/// Loads classloader histogram, classload events, statistics, and chart.
public class ClassLoadingViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<ClassloaderSummary> histogram = FXCollections.observableArrayList();
    private final ObservableList<ClassloadEvent> events = FXCollections.observableArrayList();
    private final ObservableList<ClassloaderStatistics> statistics = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> chart = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public ClassLoadingViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
    }

    public ObservableList<ClassloaderSummary> histogram() {
        return histogram;
    }

    public ObservableList<ClassloadEvent> events() {
        return events;
    }

    public ObservableList<ClassloaderStatistics> statistics() {
        return statistics;
    }

    public ObjectProperty<ChartDefinition> chartProperty() {
        return chart;
    }

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public void load(RecordingSummary recording) {
        List<ClassloaderSummary> hist = service.loadClassloaderHistogram(recording);
        List<ClassloadEvent> evts = service.loadClassloadEvents(recording);
        List<ClassloaderStatistics> stats = service.loadClassloaderStatistics(recording);
        ChartDefinition ch = service.loadClassLoadingChart(recording);
        FxDispatch.run(() -> {
            currentRecording.set(recording);
            histogram.setAll(hist);
            events.setAll(evts);
            statistics.setAll(stats);
            chart.set(ch);
        });
    }
}
