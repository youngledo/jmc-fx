package com.youngledo.jmcfx.ui.tlab;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.domain.service.TlabService;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TlabViewModel {

    private final TlabService tlabService;
    private final ObservableList<TlabAllocation> allocations = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private final ObjectProperty<TlabAllocation> selectedAllocation = new SimpleObjectProperty<>();

    public TlabViewModel(TlabService tlabService) {
        this.tlabService = tlabService;
    }

    public ObservableList<TlabAllocation> allocationsProperty() {
        return allocations;
    }

    public ObjectProperty<ChartDefinition> timelineProperty() {
        return timeline;
    }

    public ObjectProperty<TlabAllocation> selectedAllocationProperty() {
        return selectedAllocation;
    }

    public void load(RecordingSummary recording) {
        List<TlabAllocation> data = tlabService.loadTlabAllocations(recording);
        ChartDefinition chart = tlabService.loadTlabAllocationTimeline(recording);
        FxDispatch.run(() -> {
            allocations.setAll(data);
            timeline.set(chart);
            selectedAllocation.set(null);
        });
    }
}
