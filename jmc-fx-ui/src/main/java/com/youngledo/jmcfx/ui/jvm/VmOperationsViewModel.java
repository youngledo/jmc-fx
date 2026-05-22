package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.VmOperationEvent;
import com.youngledo.jmcfx.domain.model.VmOperationSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the VM Operations page.
///
/// Loads VM operation summary and event data from a recording.
public class VmOperationsViewModel {

    private final JvmInternalsService service;
    private final ObservableList<VmOperationSummary> summary = FXCollections.observableArrayList();
    private final ObservableList<VmOperationEvent> events = FXCollections.observableArrayList();

    public VmOperationsViewModel(JvmInternalsService service) {
        this.service = service;
    }

    public ObservableList<VmOperationSummary> summary() {
        return summary;
    }

    public ObservableList<VmOperationEvent> events() {
        return events;
    }

    public void load(RecordingSummary recording) {
        Thread.startVirtualThread(() -> {
            List<VmOperationSummary> summ = service.loadVmOperationSummary(recording);
            List<VmOperationEvent> evts = service.loadVmOperationEvents(recording);
            Platform.runLater(() -> {
                summary.setAll(summ);
                events.setAll(evts);
            });
        });
    }
}
