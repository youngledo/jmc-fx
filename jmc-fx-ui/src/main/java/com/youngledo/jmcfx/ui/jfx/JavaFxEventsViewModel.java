package com.youngledo.jmcfx.ui.jfx;

import java.time.ZoneId;
import java.util.StringJoiner;

import com.youngledo.jmcfx.domain.model.JavaFxEventReport;
import com.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import com.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import com.youngledo.jmcfx.domain.model.JavaFxPulseSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;
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

public class JavaFxEventsViewModel {

    private final JavaFxEventService service;
    private final ObservableList<JavaFxPulseSummary> pulseSummaries = FXCollections.observableArrayList();
    private final ObservableList<JavaFxPulsePhase> pulsePhases = FXCollections.observableArrayList();
    private final ObservableList<JavaFxInputEvent> inputEvents = FXCollections.observableArrayList();
    private final ObjectProperty<JavaFxPulsePhase> selectedPulsePhase = new SimpleObjectProperty<>();
    private final StringProperty summary = new SimpleStringProperty("");
    private final StringProperty selectedDetail = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty error = new SimpleBooleanProperty(false);

    public JavaFxEventsViewModel(JavaFxEventService service) {
        this.service = service;
        selectedPulsePhase.addListener((observable, oldValue, newValue) -> selectedDetail.set(detailText(newValue)));
    }

    public ObservableList<JavaFxPulseSummary> pulseSummariesProperty() {
        return pulseSummaries;
    }

    public ObservableList<JavaFxPulsePhase> pulsePhasesProperty() {
        return pulsePhases;
    }

    public ObservableList<JavaFxInputEvent> inputEventsProperty() {
        return inputEvents;
    }

    public ObjectProperty<JavaFxPulsePhase> selectedPulsePhaseProperty() {
        return selectedPulsePhase;
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
            JavaFxEventReport report = service.loadJavaFxEvents(recording);
            FxDispatch.run(() -> {
                pulseSummaries.setAll(report.pulseSummaries());
                pulsePhases.setAll(report.pulsePhases());
                inputEvents.setAll(report.inputEvents());
                selectedPulsePhase.set(pulsePhases.isEmpty() ? null : pulsePhases.getFirst());
                summary.set(summaryText(report));
                loading.set(false);
            });
        } catch (RuntimeException exception) {
            FxDispatch.run(() -> {
                pulseSummaries.clear();
                pulsePhases.clear();
                inputEvents.clear();
                selectedPulsePhase.set(null);
                summary.set("");
                error.set(true);
                errorMessage.set(exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage());
                loading.set(false);
            });
        }
    }

    private static String summaryText(JavaFxEventReport report) {
        return DisplayFormats.formatInteger(report.pulseCount()) + " pulses, "
                + DisplayFormats.formatInteger(report.phaseCount()) + " phases, "
                + DisplayFormats.formatInteger(report.inputCount()) + " input events, "
                + DisplayFormats.formatInteger(report.slowPhaseCount()) + " slow phases";
    }

    private static String detailText(JavaFxPulsePhase phase) {
        if (phase == null) {
            return "";
        }
        StringJoiner detail = new StringJoiner(System.lineSeparator());
        detail.add("Pulse " + DisplayFormats.formatInteger(phase.pulseId()));
        detail.add("Phase: " + phase.phaseName());
        detail.add("Duration: " + DisplayFormats.formatMicros(phase.durationMicros()));
        detail.add("Thread: " + phase.threadName());
        detail.add("Time: " + DisplayFormats.formatTimestamp(phase.startTime(), ZoneId.systemDefault()));
        return detail.toString();
    }
}
