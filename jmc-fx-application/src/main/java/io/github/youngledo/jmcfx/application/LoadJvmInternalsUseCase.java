package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import io.github.youngledo.jmcfx.domain.model.ClassloaderSummary;
import io.github.youngledo.jmcfx.domain.model.ClassloadEvent;
import io.github.youngledo.jmcfx.domain.model.CodeCacheStats;
import io.github.youngledo.jmcfx.domain.model.CodeCacheSweep;
import io.github.youngledo.jmcfx.domain.model.CompilationEvent;
import io.github.youngledo.jmcfx.domain.model.GcConfiguration;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.GcHeapConfiguration;
import io.github.youngledo.jmcfx.domain.model.GcHeapSummary;
import io.github.youngledo.jmcfx.domain.model.GcReferenceStat;
import io.github.youngledo.jmcfx.domain.model.GcSummary;
import io.github.youngledo.jmcfx.domain.model.JvmFlag;
import io.github.youngledo.jmcfx.domain.model.JvmFlagChange;
import io.github.youngledo.jmcfx.domain.model.JvmInfo;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.VmOperationEvent;
import io.github.youngledo.jmcfx.domain.model.VmOperationSummary;
import io.github.youngledo.jmcfx.domain.service.JvmInternalsService;

public final class LoadJvmInternalsUseCase {

    private final JvmInternalsService service;

    public LoadJvmInternalsUseCase(JvmInternalsService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public JvmInfo loadJvmInfo(RecordingSummary recording) {
        return service.loadJvmInfo(recording);
    }

    public List<JvmFlag> loadJvmFlags(RecordingSummary recording) {
        return service.loadJvmFlags(recording);
    }

    public List<JvmFlagChange> loadJvmFlagChanges(RecordingSummary recording) {
        return service.loadJvmFlagChanges(recording);
    }

    public GcConfiguration loadGcConfiguration(RecordingSummary recording) {
        return service.loadGcConfiguration(recording);
    }

    public GcHeapConfiguration loadGcHeapConfiguration(RecordingSummary recording) {
        return service.loadGcHeapConfiguration(recording);
    }

    public List<GcSummary> loadGcSummaries(RecordingSummary recording) {
        return service.loadGcSummaries(recording);
    }

    public List<GcEvent> loadGcEvents(RecordingSummary recording) {
        return service.loadGcEvents(recording);
    }

    public ChartDefinition loadGcHeapChart(RecordingSummary recording) {
        return service.loadGcHeapChart(recording);
    }

    public ChartDefinition loadGcMetaspaceChart(RecordingSummary recording) {
        return service.loadGcMetaspaceChart(recording);
    }

    public ChartDefinition loadGcPauseChart(RecordingSummary recording) {
        return service.loadGcPauseChart(recording);
    }

    public List<GcReferenceStat> loadGcReferenceStats(RecordingSummary recording) {
        return service.loadGcReferenceStats(recording);
    }

    public List<GcHeapSummary> loadGcHeapSummaries(RecordingSummary recording) {
        return service.loadGcHeapSummaries(recording);
    }

    public List<CompilationEvent> loadCompilationEvents(RecordingSummary recording) {
        return service.loadCompilationEvents(recording);
    }

    public List<CompilationEvent> loadCompilationFailures(RecordingSummary recording) {
        return service.loadCompilationFailures(recording);
    }

    public ChartDefinition loadCompilationDurationChart(RecordingSummary recording) {
        return service.loadCompilationDurationChart(recording);
    }

    public List<CodeCacheSweep> loadCodeCacheSweeps(RecordingSummary recording) {
        return service.loadCodeCacheSweeps(recording);
    }

    public List<CodeCacheStats> loadCodeCacheStatistics(RecordingSummary recording) {
        return service.loadCodeCacheStatistics(recording);
    }

    public ChartDefinition loadCodeCacheEntriesChart(RecordingSummary recording) {
        return service.loadCodeCacheEntriesChart(recording);
    }

    public ChartDefinition loadCodeCacheSweepChart(RecordingSummary recording) {
        return service.loadCodeCacheSweepChart(recording);
    }

    public List<ClassloaderSummary> loadClassloaderHistogram(RecordingSummary recording) {
        return service.loadClassloaderHistogram(recording);
    }

    public List<ClassloadEvent> loadClassloadEvents(RecordingSummary recording) {
        return service.loadClassloadEvents(recording);
    }

    public List<ClassloaderStatistics> loadClassloaderStatistics(RecordingSummary recording) {
        return service.loadClassloaderStatistics(recording);
    }

    public ChartDefinition loadClassLoadingChart(RecordingSummary recording) {
        return service.loadClassLoadingChart(recording);
    }

    public List<VmOperationSummary> loadVmOperationSummary(RecordingSummary recording) {
        return service.loadVmOperationSummary(recording);
    }

    public List<VmOperationEvent> loadVmOperationEvents(RecordingSummary recording) {
        return service.loadVmOperationEvents(recording);
    }
}
