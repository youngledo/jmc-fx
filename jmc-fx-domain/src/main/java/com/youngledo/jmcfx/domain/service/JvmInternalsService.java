package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import com.youngledo.jmcfx.domain.model.ClassloaderSummary;
import com.youngledo.jmcfx.domain.model.ClassloadEvent;
import com.youngledo.jmcfx.domain.model.CodeCacheStats;
import com.youngledo.jmcfx.domain.model.CodeCacheSweep;
import com.youngledo.jmcfx.domain.model.CompilationEvent;
import com.youngledo.jmcfx.domain.model.GcConfiguration;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.GcHeapConfiguration;
import com.youngledo.jmcfx.domain.model.GcHeapSummary;
import com.youngledo.jmcfx.domain.model.GcReferenceStat;
import com.youngledo.jmcfx.domain.model.GcSummary;
import com.youngledo.jmcfx.domain.model.JvmFlag;
import com.youngledo.jmcfx.domain.model.JvmFlagChange;
import com.youngledo.jmcfx.domain.model.JvmInfo;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.VmOperationEvent;
import com.youngledo.jmcfx.domain.model.VmOperationSummary;

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
