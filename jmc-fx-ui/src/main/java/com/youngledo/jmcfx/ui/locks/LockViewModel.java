package com.youngledo.jmcfx.ui.locks;

import java.util.List;

import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.LockService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Lock Instances page.
///
/// Manages three linked histograms (by class, by address, by thread) with
/// chain filtering. Selecting a row in one histogram filters the others.
public class LockViewModel {

    private final LockService lockService;
    private final ObservableList<LockHistogram> classHistogram = FXCollections.observableArrayList();
    private final ObservableList<LockHistogram> addressHistogram = FXCollections.observableArrayList();
    private final ObservableList<LockHistogram> threadHistogram = FXCollections.observableArrayList();
    private final ObjectProperty<LockGrouping> primaryGrouping =
            new SimpleObjectProperty<>(LockGrouping.BY_CLASS);
    private RecordingSummary currentRecording;

    public LockViewModel(LockService lockService) {
        this.lockService = lockService;
    }

    public ObservableList<LockHistogram> classHistogramProperty() {
        return classHistogram;
    }

    public ObservableList<LockHistogram> addressHistogramProperty() {
        return addressHistogram;
    }

    public ObservableList<LockHistogram> threadHistogramProperty() {
        return threadHistogram;
    }

    public ObjectProperty<LockGrouping> primaryGroupingProperty() {
        return primaryGrouping;
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        reloadAll();
    }

    public void setPrimaryGrouping(LockGrouping grouping) {
        primaryGrouping.set(grouping);
    }

    public void reloadAll() {
        if (currentRecording == null) {
            return;
        }
        classHistogram.setAll(lockService.loadLockHistogram(currentRecording, LockGrouping.BY_CLASS));
        addressHistogram.setAll(lockService.loadLockHistogram(currentRecording, LockGrouping.BY_ADDRESS));
        threadHistogram.setAll(lockService.loadLockHistogram(currentRecording, LockGrouping.BY_THREAD));
    }
}
