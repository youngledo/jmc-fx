package com.youngledo.jmcfx.ui.jvm;

import java.util.List;

import com.youngledo.jmcfx.domain.model.GcConfiguration;
import com.youngledo.jmcfx.domain.model.GcHeapConfiguration;
import com.youngledo.jmcfx.domain.model.KeyValueEntry;
import com.youngledo.jmcfx.domain.model.KeyValueSection;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;

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
                            new KeyValueEntry("Parallel GC Threads", String.valueOf(gcConfig.parallelGcThreads())),
                            new KeyValueEntry("Concurrent GC Threads", String.valueOf(gcConfig.concurrentGcThreads())),
                            new KeyValueEntry("Explicit GC Concurrent", String.valueOf(gcConfig.explicitGcConcurrent())),
                            new KeyValueEntry("Explicit GC Disabled", String.valueOf(gcConfig.explicitGcDisabled())),
                            new KeyValueEntry("Use Dynamic GC Threads", String.valueOf(gcConfig.useDynamicGcThreads())),
                            new KeyValueEntry("GC Time Ratio", String.valueOf(gcConfig.gcTimeRatio())))),
                    new KeyValueSection("Heap Configuration", List.of(
                            new KeyValueEntry("Min Size", formatBytes(heapConfig.minSize())),
                            new KeyValueEntry("Max Size", formatBytes(heapConfig.maxSize())),
                            new KeyValueEntry("Initial Size", formatBytes(heapConfig.initialSize())),
                            new KeyValueEntry("Object Alignment", heapConfig.objectAlignment() + " bytes"),
                            new KeyValueEntry("Address Size", heapConfig.addressSize() + " bits"),
                            new KeyValueEntry("Use Compressed Oops", String.valueOf(heapConfig.useCompressedOops())),
                            new KeyValueEntry("Compressed Oops Mode", heapConfig.compressedOopsMode()))));
            Platform.runLater(() -> configSections.setAll(sections));
        });
    }

    public ObservableList<KeyValueSection> configSections() {
        return configSections;
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        if (bytes < 1024L * 1024 * 1024) {
            return (bytes / (1024 * 1024)) + " MB";
        }
        return (bytes / (1024L * 1024 * 1024)) + " GB";
    }
}
