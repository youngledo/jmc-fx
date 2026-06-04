package com.youngledo.jmcfx.ui.metadata;

import java.util.StringJoiner;

import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import com.youngledo.jmcfx.domain.model.JfrMetadataField;
import com.youngledo.jmcfx.domain.model.JfrMetadataReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.application.LoadJfrMetadataUseCase;
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

public class JfrMetadataViewModel {

    private final LoadJfrMetadataUseCase service;
    private final ObservableList<JfrMetadataEventType> eventTypes = FXCollections.observableArrayList();
    private final ObjectProperty<JfrMetadataEventType> selectedEventType = new SimpleObjectProperty<>();
    private final StringProperty summary = new SimpleStringProperty("");
    private final StringProperty selectedDetail = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty error = new SimpleBooleanProperty(false);

    public JfrMetadataViewModel(LoadJfrMetadataUseCase service) {
        this.service = service;
        selectedEventType.addListener((observable, oldValue, newValue) -> selectedDetail.set(detailText(newValue)));
    }

    public ObservableList<JfrMetadataEventType> eventTypesProperty() {
        return eventTypes;
    }

    public ObjectProperty<JfrMetadataEventType> selectedEventTypeProperty() {
        return selectedEventType;
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
            JfrMetadataReport report = service.loadMetadata(recording);
            FxDispatch.run(() -> {
                eventTypes.setAll(report.eventTypes());
                selectedEventType.set(eventTypes.isEmpty() ? null : eventTypes.getFirst());
                summary.set(summaryText(report));
                loading.set(false);
            });
        } catch (RuntimeException exception) {
            FxDispatch.run(() -> {
                eventTypes.clear();
                selectedEventType.set(null);
                summary.set("");
                error.set(true);
                errorMessage.set(exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage());
                loading.set(false);
            });
        }
    }

    private static String summaryText(JfrMetadataReport report) {
        return DisplayFormats.formatInteger(report.eventTypeCount()) + " event types, "
                + DisplayFormats.formatInteger(report.eventCount()) + " events, "
                + DisplayFormats.formatInteger(report.fieldCount()) + " fields";
    }

    private static String detailText(JfrMetadataEventType eventType) {
        if (eventType == null) {
            return "";
        }

        StringJoiner detail = new StringJoiner(System.lineSeparator());
        detail.add(eventType.name() + " (" + eventType.id() + ")");
        detail.add("Category: " + eventType.category());
        detail.add("Events: " + DisplayFormats.formatInteger(eventType.eventCount()));
        if (!eventType.description().isBlank()) {
            detail.add("Description: " + eventType.description());
        }
        detail.add("");
        detail.add("Fields");
        for (JfrMetadataField field : eventType.fields()) {
            String unit = field.unit().isBlank() ? "" : " [" + field.unit() + "]";
            detail.add("- " + field.label() + " (" + field.id() + "): " + field.valueType() + unit);
            detail.add("  Type: " + field.valueType());
            detail.add("  Filter value type: " + field.valueType());
            if (!field.unit().isBlank()) {
                detail.add("  Unit: " + field.unit());
            }
            if (!field.description().isBlank()) {
                detail.add("  " + field.description());
            }
        }
        return detail.toString();
    }
}
