package io.github.youngledo.jmcfx.ui.javaapp;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.application.LoadJavaApplicationUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Native Library page.
///
/// Manages native library table data from jdk.NativeLibrary events.
public class NativeLibraryViewModel {

    private final LoadJavaApplicationUseCase javaAppService;
    private final ObservableList<NativeLibraryEntry> libraries = FXCollections.observableArrayList();
    private final ObjectProperty<NativeLibraryEntry> selectedLibrary = new SimpleObjectProperty<>();

    public NativeLibraryViewModel(LoadJavaApplicationUseCase javaAppService) {
        this.javaAppService = javaAppService;
    }

    public ObservableList<NativeLibraryEntry> librariesProperty() {
        return libraries;
    }

    public ObjectProperty<NativeLibraryEntry> selectedLibraryProperty() {
        return selectedLibrary;
    }

    /// Loads native library events for the given recording.
    ///
    /// @param recording the flight recording to analyze
    public void load(RecordingSummary recording) {
        List<NativeLibraryEntry> entries = javaAppService.loadNativeLibraries(recording);
        FxDispatch.run(() -> {
            libraries.setAll(entries);
            selectedLibrary.set(null);
        });
    }
}
