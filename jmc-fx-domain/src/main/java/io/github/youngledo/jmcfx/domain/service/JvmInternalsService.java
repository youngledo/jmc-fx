package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

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

/// Unified service port for all JVM internals queries.
///
/// Each method loads a specific slice of JVM runtime data from a JFR recording.
/// Methods return empty collections when the recording contains no matching events.
public interface JvmInternalsService {

    // 5A: JVM Information
    JvmInfo loadJvmInfo(RecordingSummary recording);
    List<JvmFlag> loadJvmFlags(RecordingSummary recording);
    List<JvmFlagChange> loadJvmFlagChanges(RecordingSummary recording);

    // 5B: GC Configuration
    GcConfiguration loadGcConfiguration(RecordingSummary recording);
    GcHeapConfiguration loadGcHeapConfiguration(RecordingSummary recording);

    // 5C: GC Summary
    List<GcSummary> loadGcSummaries(RecordingSummary recording);

    // 5D: GC Details
    List<GcEvent> loadGcEvents(RecordingSummary recording);
    ChartDefinition loadGcHeapChart(RecordingSummary recording);
    ChartDefinition loadGcMetaspaceChart(RecordingSummary recording);
    ChartDefinition loadGcPauseChart(RecordingSummary recording);
    List<GcReferenceStat> loadGcReferenceStats(RecordingSummary recording);
    List<GcHeapSummary> loadGcHeapSummaries(RecordingSummary recording);

    // 5E: Compilations
    List<CompilationEvent> loadCompilationEvents(RecordingSummary recording);
    List<CompilationEvent> loadCompilationFailures(RecordingSummary recording);
    ChartDefinition loadCompilationDurationChart(RecordingSummary recording);

    // 5F: Code Cache
    List<CodeCacheSweep> loadCodeCacheSweeps(RecordingSummary recording);
    List<CodeCacheStats> loadCodeCacheStatistics(RecordingSummary recording);
    ChartDefinition loadCodeCacheEntriesChart(RecordingSummary recording);
    ChartDefinition loadCodeCacheSweepChart(RecordingSummary recording);

    // 5G: Class Loading
    List<ClassloaderSummary> loadClassloaderHistogram(RecordingSummary recording);
    List<ClassloadEvent> loadClassloadEvents(RecordingSummary recording);
    List<ClassloaderStatistics> loadClassloaderStatistics(RecordingSummary recording);
    ChartDefinition loadClassLoadingChart(RecordingSummary recording);

    // 5H: VM Operations
    List<VmOperationSummary> loadVmOperationSummary(RecordingSummary recording);
    List<VmOperationEvent> loadVmOperationEvents(RecordingSummary recording);
}
