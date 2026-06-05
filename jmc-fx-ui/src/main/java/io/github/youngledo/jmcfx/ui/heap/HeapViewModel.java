package io.github.youngledo.jmcfx.ui.heap;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadHeapUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class HeapViewModel {

    private final LoadHeapUseCase heapService;
    private final ObservableList<HeapClassHistogram> histogram = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private final ObjectProperty<HeapClassHistogram> selectedRow = new SimpleObjectProperty<>();

    public HeapViewModel(LoadHeapUseCase heapService) {
        this.heapService = heapService;
    }

    public ObservableList<HeapClassHistogram> histogramProperty() {
        return histogram;
    }

    public ObjectProperty<ChartDefinition> timelineProperty() {
        return timeline;
    }

    public ObjectProperty<HeapClassHistogram> selectedRowProperty() {
        return selectedRow;
    }

    public void load(RecordingSummary recording) {
        List<HeapClassHistogram> data = heapService.loadHeapClassHistogram(recording);
        ChartDefinition chart = heapService.loadHeapUsageTimeline(recording);
        FxDispatch.run(() -> {
            histogram.setAll(data);
            timeline.set(chart);
            selectedRow.set(null);
        });
    }
}
