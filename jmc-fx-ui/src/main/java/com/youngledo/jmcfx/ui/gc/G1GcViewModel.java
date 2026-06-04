package com.youngledo.jmcfx.ui.gc;

import java.time.ZoneId;
import java.util.StringJoiner;

import com.youngledo.jmcfx.domain.model.G1GcRegionState;
import com.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import com.youngledo.jmcfx.domain.model.G1GcReport;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.application.LoadG1GcUseCase;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class G1GcViewModel {

    private final LoadG1GcUseCase service;
    private final ObservableList<G1GcRegionSummary> regionSummaries = FXCollections.observableArrayList();
    private final ObservableList<G1GcRegionState> recentRegionStates = FXCollections.observableArrayList();
    private final ObservableList<GcEvent> gcPauses = FXCollections.observableArrayList();
    private final ObjectProperty<G1GcRegionState> selectedRegionState = new SimpleObjectProperty<>();
    private final StringProperty summary = new SimpleStringProperty("");
    private final StringProperty selectedDetail = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty error = new SimpleBooleanProperty(false);

    public G1GcViewModel(LoadG1GcUseCase service) {
        this.service = service;
        selectedRegionState.addListener((observable, oldValue, newValue) -> selectedDetail.set(detailText(newValue)));
    }

    public ObservableList<G1GcRegionSummary> regionSummariesProperty() {
        return regionSummaries;
    }

    public ObservableList<G1GcRegionState> recentRegionStatesProperty() {
        return recentRegionStates;
    }

    public ObservableList<GcEvent> gcPausesProperty() {
        return gcPauses;
    }

    public ObjectProperty<G1GcRegionState> selectedRegionStateProperty() {
        return selectedRegionState;
    }

    public StringProperty summaryProperty() {
        return summary;
    }

    public StringProperty selectedDetailProperty() {
        return selectedDetail;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public BooleanProperty errorProperty() {
        return error;
    }

    public void load(RecordingSummary recording) {
        loading.set(true);
        error.set(false);
        errorMessage.set("");
        try {
            G1GcReport report = service.loadG1GcReport(recording);
            FxDispatch.run(() -> {
                regionSummaries.setAll(report.regionSummaries());
                recentRegionStates.setAll(report.recentRegionStates());
                gcPauses.setAll(report.gcPauses());
                selectedRegionState.set(recentRegionStates.isEmpty() ? null : recentRegionStates.getFirst());
                summary.set(summaryText(report));
                loading.set(false);
            });
        } catch (RuntimeException exception) {
            FxDispatch.run(() -> {
                regionSummaries.clear();
                recentRegionStates.clear();
                gcPauses.clear();
                selectedRegionState.set(null);
                summary.set("");
                error.set(true);
                errorMessage.set(exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage());
                loading.set(false);
            });
        }
    }

    private static String summaryText(G1GcReport report) {
        return DisplayFormats.formatInteger(report.snapshotCount()) + " snapshots, "
                + DisplayFormats.formatInteger(report.transitionCount()) + " transitions, "
                + DisplayFormats.formatInteger(report.gcPauseCount()) + " GC pauses, "
                + DisplayFormats.formatInteger(report.regionCount()) + " regions";
    }

    private static String detailText(G1GcRegionState state) {
        if (state == null) {
            return "";
        }
        StringJoiner detail = new StringJoiner(System.lineSeparator());
        detail.add("Region " + DisplayFormats.formatInteger(state.regionIndex()));
        if (state.previousType().isBlank()) {
            detail.add("Type: " + state.type());
        } else {
            detail.add("Type: " + state.previousType() + " -> " + state.type());
        }
        detail.add("Event: " + state.eventKind());
        detail.add("Used: " + DisplayFormats.formatFileSize(state.usedBytes()));
        detail.add("Capacity: " + DisplayFormats.formatFileSize(state.capacityBytes()));
        detail.add("Allocation Context: " + DisplayFormats.formatInteger(state.allocationContext()));
        detail.add("Time: " + DisplayFormats.formatTimestamp(state.startTime(), ZoneId.systemDefault()));
        return detail.toString();
    }
}
