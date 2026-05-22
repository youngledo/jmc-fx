package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.GcSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the GC Summary page.
///
/// Loads per-generation GC summary data from a recording.
public class GcSummaryViewModel {

    private final JvmInternalsService service;
    private final ObservableList<GcSummary> summaries = FXCollections.observableArrayList();

    public GcSummaryViewModel(JvmInternalsService service) {
        this.service = service;
    }

    public ObservableList<GcSummary> summaries() {
        return summaries;
    }

    public void load(RecordingSummary recording) {
        Thread.startVirtualThread(() -> {
            List<GcSummary> data = service.loadGcSummaries(recording);
            Platform.runLater(() -> summaries.setAll(data));
        });
    }
}
