package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.VmOperationEvent;
import io.github.youngledo.jmcfx.domain.model.VmOperationSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the VM Operations page.
///
/// Loads VM operation summary and event data from a recording.
public class VmOperationsViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<VmOperationSummary> summary = FXCollections.observableArrayList();
    private final ObservableList<VmOperationEvent> events = FXCollections.observableArrayList();

    public VmOperationsViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
    }

    public ObservableList<VmOperationSummary> summary() {
        return summary;
    }

    public ObservableList<VmOperationEvent> events() {
        return events;
    }

    public void load(RecordingSummary recording) {
        List<VmOperationSummary> summ = service.loadVmOperationSummary(recording);
        List<VmOperationEvent> evts = service.loadVmOperationEvents(recording);
        FxDispatch.run(() -> {
            summary.setAll(summ);
            events.setAll(evts);
        });
    }
}
