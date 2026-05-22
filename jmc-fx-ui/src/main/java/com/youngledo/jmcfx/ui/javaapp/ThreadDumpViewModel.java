package com.youngledo.jmcfx.ui.javaapp;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.service.JavaAppService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Thread Dumps page.
///
/// Manages thread dump entries from jdk.ThreadDump events.
/// When a dump is selected, its full text is shown in a detail pane.
public class ThreadDumpViewModel {

    private final JavaAppService javaAppService;
    private final ObservableList<ThreadDumpEntry> dumps = FXCollections.observableArrayList();
    private final ObjectProperty<ThreadDumpEntry> selectedDump = new SimpleObjectProperty<>();
    private final StringProperty dumpText = new SimpleStringProperty("");

    public ThreadDumpViewModel(JavaAppService javaAppService) {
        this.javaAppService = javaAppService;
        selectedDump.addListener((obs, oldVal, newVal) -> {
            dumpText.set(newVal != null ? newVal.dumpText() : "");
        });
    }

    public ObservableList<ThreadDumpEntry> dumpsProperty() {
        return dumps;
    }

    public ObjectProperty<ThreadDumpEntry> selectedDumpProperty() {
        return selectedDump;
    }

    public StringProperty dumpTextProperty() {
        return dumpText;
    }

    /// Loads thread dump events for the given recording.
    ///
    /// @param recording the flight recording to analyze
    public void load(RecordingSummary recording) {
        List<ThreadDumpEntry> entries = javaAppService.loadThreadDumps(recording);
        dumps.setAll(entries);
        selectedDump.set(null);
        dumpText.set("");
    }
}
