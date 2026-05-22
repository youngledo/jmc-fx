package com.youngledo.jmcfx.ui.heap;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.HeapService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class HeapViewModel {

    private final HeapService heapService;
    private final ObservableList<HeapClassHistogram> histogram = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private final ObjectProperty<HeapClassHistogram> selectedRow = new SimpleObjectProperty<>();

    public HeapViewModel(HeapService heapService) {
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
        histogram.setAll(data);
        timeline.set(heapService.loadHeapUsageTimeline(recording));
        selectedRow.set(null);
    }
}
