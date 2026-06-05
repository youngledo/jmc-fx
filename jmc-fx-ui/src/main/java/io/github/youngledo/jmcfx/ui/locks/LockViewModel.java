package io.github.youngledo.jmcfx.ui.locks;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadLocksUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Lock Instances page.
///
/// Manages three linked histograms (by class, by address, by thread) with
/// chain filtering. Selecting a row in one histogram filters the others.
public class LockViewModel {

    private final LoadLocksUseCase lockService;
    private final ObservableList<LockHistogram> classHistogram = FXCollections.observableArrayList();
    private final ObservableList<LockHistogram> addressHistogram = FXCollections.observableArrayList();
    private final ObservableList<LockHistogram> threadHistogram = FXCollections.observableArrayList();
    private final ObjectProperty<LockGrouping> primaryGrouping =
            new SimpleObjectProperty<>(LockGrouping.BY_CLASS);
    private RecordingSummary currentRecording;

    public LockViewModel(LoadLocksUseCase lockService) {
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
        List<LockHistogram> byClass = lockService.loadLockHistogram(currentRecording, LockGrouping.BY_CLASS);
        List<LockHistogram> byAddress = lockService.loadLockHistogram(currentRecording, LockGrouping.BY_ADDRESS);
        List<LockHistogram> byThread = lockService.loadLockHistogram(currentRecording, LockGrouping.BY_THREAD);
        FxDispatch.run(() -> {
            classHistogram.setAll(byClass);
            addressHistogram.setAll(byAddress);
            threadHistogram.setAll(byThread);
        });
    }
}
