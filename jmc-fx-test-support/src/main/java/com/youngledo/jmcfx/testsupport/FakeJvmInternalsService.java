package com.youngledo.jmcfx.testsupport;

import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDataPoint;
import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;
import com.youngledo.jmcfx.domain.model.ChartSeriesType;
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
import com.youngledo.jmcfx.domain.service.JvmInternalsService;

/// Fake implementation of JvmInternalsService for UI tests.
///
/// Returns deterministic sample data for all JVM internals queries.
public class FakeJvmInternalsService implements JvmInternalsService {

    @Override
    public JvmInfo loadJvmInfo(RecordingSummary recording) {
        return new JvmInfo("OpenJDK 64-Bit Server VM", "25+1", "-XX:+UseG1GC", 12345);
    }

    @Override
    public List<JvmFlag> loadJvmFlags(RecordingSummary recording) {
        return List.of(
                new JvmFlag("UseG1GC", "true", "VM"),
                new JvmFlag("MaxHeapSize", "536870912", "ergonomic"),
                new JvmFlag("PrintGCDetails", "false", "default"));
    }

    @Override
    public List<JvmFlagChange> loadJvmFlagChanges(RecordingSummary recording) {
        return List.of(
                new JvmFlagChange(Instant.parse("2026-01-01T00:00:01Z"),
                        "MaxHeapSize", "268435456", "536870912", "ergonomic"));
    }

    @Override
    public GcConfiguration loadGcConfiguration(RecordingSummary recording) {
        return new GcConfiguration("G1New", "", 4, 2, false, false, false, 12);
    }

    @Override
    public GcHeapConfiguration loadGcHeapConfiguration(RecordingSummary recording) {
        return new GcHeapConfiguration(8388608, 536870912, 536870912, 8, 64, true, "32-bit");
    }

    @Override
    public List<GcSummary> loadGcSummaries(RecordingSummary recording) {
        return List.of(
                new GcSummary("young", 10, 50, 5, 12, 50),
                new GcSummary("old", 2, 200, 100, 120, 200));
    }

    @Override
    public List<GcEvent> loadGcEvents(RecordingSummary recording) {
        return List.of(
                new GcEvent(1, "G1New", "G1 Evacuation Pause", 5000, 8000, Instant.parse("2026-01-01T00:00:01Z")),
                new GcEvent(2, "G1Old", "G1 Full GC", 120000, 200000, Instant.parse("2026-01-01T00:00:10Z")));
    }

    @Override
    public ChartDefinition loadGcHeapChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Bytes", List.of(
                new ChartSeries("used", "Used Heap", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 100_000_000), new ChartDataPoint(1, 80_000_000))),
                new ChartSeries("total", "Total Heap", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 256_000_000), new ChartDataPoint(1, 256_000_000)))));
    }

    @Override
    public ChartDefinition loadGcMetaspaceChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Bytes", List.of(
                new ChartSeries("used", "Used Metaspace", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 50_000_000)))));
    }

    @Override
    public ChartDefinition loadGcPauseChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Pause (ms)", List.of(
                new ChartSeries("gc-pause", "GC Pause", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 8), new ChartDataPoint(1, 200)))));
    }

    @Override
    public List<GcReferenceStat> loadGcReferenceStats(RecordingSummary recording) {
        return List.of(
                new GcReferenceStat(1, "Soft", 100),
                new GcReferenceStat(1, "Weak", 500));
    }

    @Override
    public List<GcHeapSummary> loadGcHeapSummaries(RecordingSummary recording) {
        return List.of(
                new GcHeapSummary(1, "After GC", 80_000_000, 256_000_000, 50_000_000, 60_000_000, 100_000_000));
    }

    @Override
    public List<CompilationEvent> loadCompilationEvents(RecordingSummary recording) {
        return List.of(
                new CompilationEvent(1, "java.lang.String.hashCode()", true, 500, 1024, 256,
                        Instant.parse("2026-01-01T00:00:05Z")),
                new CompilationEvent(2, "java.util.ArrayList.add()", true, 300, 512, 128,
                        Instant.parse("2026-01-01T00:00:06Z")));
    }

    @Override
    public List<CompilationEvent> loadCompilationFailures(RecordingSummary recording) {
        return List.of();
    }

    @Override
    public ChartDefinition loadCompilationDurationChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Duration (ms)", List.of(
                new ChartSeries("compilation-duration", "Compilation Duration", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 0.5), new ChartDataPoint(1, 0.3)))));
    }

    @Override
    public List<CodeCacheSweep> loadCodeCacheSweeps(RecordingSummary recording) {
        return List.of(
                new CodeCacheSweep(Instant.parse("2026-01-01T00:00:02Z"), 1, 100, 5, 50, 3));
    }

    @Override
    public List<CodeCacheStats> loadCodeCacheStatistics(RecordingSummary recording) {
        return List.of(
                new CodeCacheStats(Instant.parse("2026-01-01T00:00:01Z"), "codeheap", 1000, 800, 200, 50000));
    }

    @Override
    public ChartDefinition loadCodeCacheEntriesChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("entries", "Entries", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 1000))),
                new ChartSeries("methods", "Methods", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 800)))));
    }

    @Override
    public ChartDefinition loadCodeCacheSweepChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("swept", "Swept", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 50))),
                new ChartSeries("flushed", "Flushed", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 5)))));
    }

    @Override
    public List<ClassloaderSummary> loadClassloaderHistogram(RecordingSummary recording) {
        return List.of(
                new ClassloaderSummary("bootstrap", 2000, 50),
                new ClassloaderSummary("app", 1500, 30),
                new ClassloaderSummary("platform", 800, 10));
    }

    @Override
    public List<ClassloadEvent> loadClassloadEvents(RecordingSummary recording) {
        return List.of(
                new ClassloadEvent("load", Instant.parse("2026-01-01T00:00:01Z"),
                        "com.example.MyClass", "app", "app", 100),
                new ClassloadEvent("unload", Instant.parse("2026-01-01T00:00:05Z"),
                        "com.example.TempClass", "app", "", 0));
    }

    @Override
    public List<ClassloaderStatistics> loadClassloaderStatistics(RecordingSummary recording) {
        return List.of(
                new ClassloaderStatistics("app", "platform", 1500, 1024, 2048, 10));
    }

    @Override
    public ChartDefinition loadClassLoadingChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("loaded", "Loaded", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 2000), new ChartDataPoint(1, 2100))),
                new ChartSeries("unloaded", "Unloaded", ChartSeriesType.LINE,
                        List.of(new ChartDataPoint(0, 50), new ChartDataPoint(1, 55)))));
    }

    @Override
    public List<VmOperationSummary> loadVmOperationSummary(RecordingSummary recording) {
        return List.of(
                new VmOperationSummary("GC", 10, 50000, 12000),
                new VmOperationSummary("Deoptimize", 3, 500, 200));
    }

    @Override
    public List<VmOperationEvent> loadVmOperationEvents(RecordingSummary recording) {
        return List.of(
                new VmOperationEvent(Instant.parse("2026-01-01T00:00:01Z"), "GC", true, true, 12000, "GC Thread"),
                new VmOperationEvent(Instant.parse("2026-01-01T00:00:02Z"), "Deoptimize", false, false, 200, "CompilerThread"));
    }
}
