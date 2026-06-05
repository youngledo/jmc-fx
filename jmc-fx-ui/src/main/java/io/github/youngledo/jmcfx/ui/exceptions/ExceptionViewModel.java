package io.github.youngledo.jmcfx.ui.exceptions;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadExceptionsUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the v1 Exceptions page.
///
/// Manages exception histogram with configurable grouping and timeline chart.
public class ExceptionViewModel {

    private final LoadExceptionsUseCase exceptionService;
    private final ObservableList<ExceptionSummary> histogram = FXCollections.observableArrayList();
    private final ObjectProperty<ExceptionGrouping> grouping = new SimpleObjectProperty<>(ExceptionGrouping.BY_CLASS);
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private final ObjectProperty<RecordingSummary> currentRecording = new SimpleObjectProperty<>();

    public ExceptionViewModel(LoadExceptionsUseCase exceptionService) {
        this.exceptionService = exceptionService;
    }

    public ObservableList<ExceptionSummary> histogramProperty() {
        return histogram;
    }

    public ObjectProperty<ExceptionGrouping> groupingProperty() {
        return grouping;
    }

    public ObjectProperty<ChartDefinition> timelineProperty() {
        return timeline;
    }

    public ObjectProperty<RecordingSummary> currentRecordingProperty() {
        return currentRecording;
    }

    public void load(RecordingSummary recording) {
        List<ExceptionSummary> data = exceptionService.loadHistogram(recording, grouping.get());
        ChartDefinition chart = exceptionService.loadTimeline(recording);
        FxDispatch.run(() -> {
            currentRecording.set(recording);
            histogram.setAll(data);
            timeline.set(chart);
        });
    }

    public void setGrouping(ExceptionGrouping newGrouping) {
        grouping.set(newGrouping);
        if (currentRecording.get() != null) {
            reloadHistogram();
        }
    }

    private void reloadHistogram() {
        List<ExceptionSummary> data = exceptionService.loadHistogram(currentRecording.get(), grouping.get());
        FxDispatch.run(() -> histogram.setAll(data));
    }
}
