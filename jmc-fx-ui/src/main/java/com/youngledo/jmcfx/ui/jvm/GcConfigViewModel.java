package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.GcConfiguration;
import com.youngledo.jmcfx.domain.model.GcHeapConfiguration;
import com.youngledo.jmcfx.domain.model.KeyValueEntry;
import com.youngledo.jmcfx.domain.model.KeyValueSection;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the GC Configuration page.
///
/// Displays GC configuration and heap configuration as key-value sections.
public class GcConfigViewModel {

    private final JvmInternalsService service;
    private final ObservableList<KeyValueSection> configSections = FXCollections.observableArrayList();

    public GcConfigViewModel(JvmInternalsService service) {
        this.service = service;
    }

    public void load(RecordingSummary recording) {
        Thread.startVirtualThread(() -> {
            GcConfiguration gcConfig = service.loadGcConfiguration(recording);
            GcHeapConfiguration heapConfig = service.loadGcHeapConfiguration(recording);
            List<KeyValueSection> sections = List.of(
                    new KeyValueSection("GC Configuration", List.of(
                            new KeyValueEntry("Young Collector", gcConfig.youngCollector()),
                            new KeyValueEntry("Parallel GC Threads", DisplayFormats.formatInteger(gcConfig.parallelGcThreads())),
                            new KeyValueEntry("Concurrent GC Threads", DisplayFormats.formatInteger(gcConfig.concurrentGcThreads())),
                            new KeyValueEntry("Explicit GC Concurrent", DisplayFormats.formatBoolean(gcConfig.explicitGcConcurrent())),
                            new KeyValueEntry("Explicit GC Disabled", DisplayFormats.formatBoolean(gcConfig.explicitGcDisabled())),
                            new KeyValueEntry("Use Dynamic GC Threads", DisplayFormats.formatBoolean(gcConfig.useDynamicGcThreads())),
                            new KeyValueEntry("GC Time Ratio", DisplayFormats.formatInteger(gcConfig.gcTimeRatio())))),
                    new KeyValueSection("Heap Configuration", List.of(
                            new KeyValueEntry("Min Size", DisplayFormats.formatFileSize(heapConfig.minSize())),
                            new KeyValueEntry("Max Size", DisplayFormats.formatFileSize(heapConfig.maxSize())),
                            new KeyValueEntry("Initial Size", DisplayFormats.formatFileSize(heapConfig.initialSize())),
                            new KeyValueEntry("Object Alignment", DisplayFormats.formatFileSize(heapConfig.objectAlignment())),
                            new KeyValueEntry("Address Size", heapConfig.addressSize() + " bits"),
                            new KeyValueEntry("Use Compressed Oops", DisplayFormats.formatBoolean(heapConfig.useCompressedOops())),
                            new KeyValueEntry("Compressed Oops Mode", heapConfig.compressedOopsMode()))));
            Platform.runLater(() -> configSections.setAll(sections));
        });
    }

    public ObservableList<KeyValueSection> configSections() {
        return configSections;
    }
}
