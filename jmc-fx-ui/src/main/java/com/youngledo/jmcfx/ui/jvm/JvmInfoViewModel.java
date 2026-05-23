package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JvmFlag;
import com.youngledo.jmcfx.domain.model.JvmFlagChange;
import com.youngledo.jmcfx.domain.model.JvmInfo;
import com.youngledo.jmcfx.domain.model.KeyValueEntry;
import com.youngledo.jmcfx.domain.model.KeyValueSection;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the JVM Information page.
///
/// Loads JVM info, flags, and flag changes from a recording.
public class JvmInfoViewModel {

    private final JvmInternalsService service;
    private final ObservableList<KeyValueSection> infoSections = FXCollections.observableArrayList();
    private final ObservableList<JvmFlag> flags = FXCollections.observableArrayList();
    private final ObservableList<JvmFlagChange> flagChanges = FXCollections.observableArrayList();

    public JvmInfoViewModel(JvmInternalsService service) {
        this.service = service;
    }

    public void load(RecordingSummary recording) {
        Thread.startVirtualThread(() -> {
            JvmInfo info = service.loadJvmInfo(recording);
            List<JvmFlag> flagList = service.loadJvmFlags(recording);
            List<JvmFlagChange> changes = service.loadJvmFlagChanges(recording);
            List<KeyValueSection> sections = List.of(
                    new KeyValueSection("JVM Information", List.of(
                            new KeyValueEntry("JVM Name", info.jvmName()),
                            new KeyValueEntry("JVM Version", info.jvmVersion()),
                            new KeyValueEntry("JVM Arguments", info.jvmArguments()),
                            new KeyValueEntry("PID", DisplayFormats.formatInteger(info.pid())))));
            Platform.runLater(() -> {
                infoSections.setAll(sections);
                flags.setAll(flagList);
                flagChanges.setAll(changes);
            });
        });
    }

    public ObservableList<KeyValueSection> infoSections() {
        return infoSections;
    }

    public ObservableList<JvmFlag> flags() {
        return flags;
    }

    public ObservableList<JvmFlagChange> flagChanges() {
        return flagChanges;
    }
}
