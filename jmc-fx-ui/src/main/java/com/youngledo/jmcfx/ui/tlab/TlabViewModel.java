package com.youngledo.jmcfx.ui.tlab;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.domain.service.TlabService;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TlabViewModel {

    private final TlabService tlabService;
    private final ObservableList<TlabAllocation> allocations = FXCollections.observableArrayList();
    private final ObjectProperty<ChartDefinition> timeline = new SimpleObjectProperty<>();
    private final ObjectProperty<TlabAllocation> selectedAllocation = new SimpleObjectProperty<>();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty loaded = new SimpleBooleanProperty(false);

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

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public BooleanProperty loadedProperty() {
        return loaded;
    }

    public void load(RecordingSummary recording) {
        FxDispatch.run(() -> {
            loading.set(true);
            loaded.set(false);
        });
        List<TlabAllocation> data;
        ChartDefinition chart;
        try {
            data = tlabService.loadTlabAllocations(recording);
            chart = tlabService.loadTlabAllocationTimeline(recording);
        } catch (RuntimeException exception) {
            FxDispatch.run(() -> loading.set(false));
            throw exception;
        }
        FxDispatch.run(() -> {
            allocations.setAll(data);
            timeline.set(chart);
            selectedAllocation.set(null);
            loaded.set(true);
            loading.set(false);
        });
    }
}
