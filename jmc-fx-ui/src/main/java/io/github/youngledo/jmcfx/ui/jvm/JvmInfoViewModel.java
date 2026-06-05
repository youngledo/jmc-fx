package io.github.youngledo.jmcfx.ui.jvm;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JvmFlag;
import io.github.youngledo.jmcfx.domain.model.JvmFlagChange;
import io.github.youngledo.jmcfx.domain.model.JvmInfo;
import io.github.youngledo.jmcfx.domain.model.KeyValueEntry;
import io.github.youngledo.jmcfx.domain.model.KeyValueSection;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the JVM Information page.
///
/// Loads JVM info, flags, and flag changes from a recording.
public class JvmInfoViewModel {

    private final LoadJvmInternalsUseCase service;
    private final ObservableList<KeyValueSection> infoSections = FXCollections.observableArrayList();
    private final ObservableList<JvmFlag> flags = FXCollections.observableArrayList();
    private final ObservableList<JvmFlagChange> flagChanges = FXCollections.observableArrayList();

    public JvmInfoViewModel(LoadJvmInternalsUseCase service) {
        this.service = service;
    }

    public void load(RecordingSummary recording) {
        JvmInfo info = service.loadJvmInfo(recording);
        List<JvmFlag> flagList = service.loadJvmFlags(recording);
        List<JvmFlagChange> changes = service.loadJvmFlagChanges(recording);
        List<KeyValueSection> sections = List.of(
                new KeyValueSection("JVM Information", List.of(
                        new KeyValueEntry("JVM Name", info.jvmName()),
                        new KeyValueEntry("JVM Version", info.jvmVersion()),
                        new KeyValueEntry("JVM Arguments", info.jvmArguments()),
                        new KeyValueEntry("PID", DisplayFormats.formatInteger(info.pid())))));
        FxDispatch.run(() -> {
            infoSections.setAll(sections);
            flags.setAll(flagList);
            flagChanges.setAll(changes);
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
