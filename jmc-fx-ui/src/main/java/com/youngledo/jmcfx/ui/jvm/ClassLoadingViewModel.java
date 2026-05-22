package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import com.youngledo.jmcfx.domain.model.ClassloaderSummary;
import com.youngledo.jmcfx.domain.model.ClassloadEvent;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Class Loading page.
///
/// Loads classloader histogram, classload events, statistics, and chart.
public class ClassLoadingViewModel {

    private final JvmInternalsService service;
    private final ObservableList<ClassloaderSummary> histogram = FXCollections.observableArrayList();
    private final ObservableList<ClassloadEvent> events = FXCollections.observableArrayList();
    private final ObservableList<ClassloaderStatistics> statistics = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> chart = new SimpleObjectProperty<>();

    public ClassLoadingViewModel(JvmInternalsService service) {
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

    public void load(RecordingSummary recording) {
        Thread.startVirtualThread(() -> {
            List<ClassloaderSummary> hist = service.loadClassloaderHistogram(recording);
            List<ClassloadEvent> evts = service.loadClassloadEvents(recording);
            List<ClassloaderStatistics> stats = service.loadClassloaderStatistics(recording);
            ChartDefinition ch = service.loadClassLoadingChart(recording);
            Platform.runLater(() -> {
                histogram.setAll(hist);
                events.setAll(evts);
                statistics.setAll(stats);
                chart.set(ch);
            });
        });
    }
}
