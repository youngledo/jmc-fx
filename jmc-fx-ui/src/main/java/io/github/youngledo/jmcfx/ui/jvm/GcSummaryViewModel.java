package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.GcSummary;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the GC Summary page.
///
/// Loads per-generation GC summary data from a recording.
public class GcSummaryViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<GcSummary> summaries = FXCollections.observableArrayList();

    public GcSummaryViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
    }

    public ObservableList<GcSummary> summaries() {
        return summaries;
    }

    public void load(RecordingSummary recording) {
        List<GcSummary> data = service.loadGcSummaries(recording);
        FxDispatch.run(() -> summaries.setAll(data));
    }
}
