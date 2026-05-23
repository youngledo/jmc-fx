package com.youngledo.jmcfx.ui.javaapp;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.service.JavaAppService;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Java Application Overview page.
///
/// Manages per-thread histogram rows and an XY chart definition with
/// switchable overlay series (profiling, IO, blocked, allocation, exceptions).
public class JavaAppOverviewViewModel {

    private final JavaAppService javaAppService;
    private final ObservableList<ThreadHistogramRow> histogramRows = FXCollections.observableArrayList();
    private final ObjectProperty<ThreadHistogramRow> selectedRow = new SimpleObjectProperty<>();
    private final ObjectProperty<ChartDefinition> chart = new SimpleObjectProperty<>();

    public JavaAppOverviewViewModel(JavaAppService javaAppService) {
        this.javaAppService = javaAppService;
    }

    public ObservableList<ThreadHistogramRow> histogramRowsProperty() {
        return histogramRows;
    }

    public ObjectProperty<ThreadHistogramRow> selectedRowProperty() {
        return selectedRow;
    }

    public ObjectProperty<ChartDefinition> chartProperty() {
        return chart;
    }

    /// Loads thread histogram and chart data for the given recording.
    ///
    /// @param recording the flight recording to analyze
    public void load(RecordingSummary recording) {
        List<ThreadHistogramRow> rows = javaAppService.loadThreadHistogram(recording);
        ChartDefinition chartDefinition = javaAppService.loadOverviewChart(recording);
        FxDispatch.run(() -> {
            histogramRows.setAll(rows);
            selectedRow.set(null);
            chart.set(chartDefinition);
        });
    }
}
