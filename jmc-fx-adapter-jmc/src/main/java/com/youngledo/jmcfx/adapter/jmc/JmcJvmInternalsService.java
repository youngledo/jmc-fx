package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

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
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;

/// JMC-backed adapter for all JVM internals queries.
///
/// Reads JDK flight recorder event types using the IMemberAccessor pattern
/// and converts them into domain records. No JavaFX types are used here.
public class JmcJvmInternalsService implements JvmInternalsService {

    // --- 5A: JVM Information ---

    @Override
    public JvmInfo loadJvmInfo(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection vmInfo = events.apply(ItemFilters.type(JdkTypeIDs.VM_INFO));
        IItem first = firstItem(vmInfo);
        if (first == null) {
            return new JvmInfo("", "", "", 0);
        }
        return new JvmInfo(
                readString(JdkAttributes.JVM_NAME, first),
                readString(JdkAttributes.JVM_VERSION, first),
                readString(JdkAttributes.JVM_ARGUMENTS, first),
                readLong(JdkAttributes.JVM_PID, first));
    }

    @Override
    public List<JvmFlag> loadJvmFlags(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<JvmFlag> flags = new ArrayList<>();
        String[] flagTypeIds = {
                JdkTypeIDs.BOOLEAN_FLAG, JdkTypeIDs.INT_FLAG, JdkTypeIDs.UINT_FLAG,
                JdkTypeIDs.LONG_FLAG, JdkTypeIDs.ULONG_FLAG, JdkTypeIDs.DOUBLE_FLAG,
                JdkTypeIDs.STRING_FLAG
        };
        for (String typeId : flagTypeIds) {
            IItemCollection flagEvents = events.apply(ItemFilters.type(typeId));
            flagEvents.stream().flatMap(IItemIterable::stream).forEach(item -> {
                String name = readString(JdkAttributes.FLAG_NAME, item);
                String value = readFlagValue(item);
                String origin = readString(JdkAttributes.FLAG_ORIGIN, item);
                flags.add(new JvmFlag(name, value, origin));
            });
        }
        flags.sort(Comparator.comparing(JvmFlag::name));
        return List.copyOf(flags);
    }

    @Override
    public List<JvmFlagChange> loadJvmFlagChanges(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<JvmFlagChange> changes = new ArrayList<>();
        String[] changeTypeIds = {
                JdkTypeIDs.BOOLEAN_FLAG_CHANGED, JdkTypeIDs.INT_FLAG_CHANGED,
                JdkTypeIDs.UINT_FLAG_CHANGED, JdkTypeIDs.LONG_FLAG_CHANGED,
                JdkTypeIDs.ULONG_FLAG_CHANGED, JdkTypeIDs.DOUBLE_FLAG_CHANGED,
                JdkTypeIDs.STRING_FLAG_CHANGED
        };
        for (String typeId : changeTypeIds) {
            IItemCollection changeEvents = events.apply(ItemFilters.type(typeId));
            changeEvents.stream().flatMap(IItemIterable::stream).forEach(item -> {
                changes.add(new JvmFlagChange(
                        readInstant(JfrAttributes.START_TIME, item),
                        readString(JdkAttributes.FLAG_NAME, item),
                        readOldFlagValue(item),
                        readNewFlagValue(item),
                        readString(JdkAttributes.FLAG_ORIGIN, item)));
            });
        }
        changes.sort(Comparator.comparing(JvmFlagChange::startTime));
        return List.copyOf(changes);
    }

    // --- 5B: GC Configuration ---

    @Override
    public GcConfiguration loadGcConfiguration(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItem first = firstItem(events.apply(JdkFilters.GC_CONFIG));
        if (first == null) {
            return new GcConfiguration("", "", 0, 0, false, false, false, 0);
        }
        return new GcConfiguration(
                readString(JdkAttributes.YOUNG_COLLECTOR, first),
                readString(JdkAttributes.OLD_COLLECTOR, first),
                readLong(JdkAttributes.PARALLEL_GC_THREADS, first),
                readLong(JdkAttributes.CONCURRENT_GC_THREADS, first),
                readBoolean(JdkAttributes.EXPLICIT_GC_CONCURRENT, first),
                readBoolean(JdkAttributes.EXPLICIT_GC_DISABLED, first),
                readBoolean(JdkAttributes.USE_DYNAMIC_GC_THREADS, first),
                readLong(JdkAttributes.GC_TIME_RATIO, first));
    }

    @Override
    public GcHeapConfiguration loadGcHeapConfiguration(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItem first = firstItem(events.apply(JdkFilters.HEAP_CONFIG));
        if (first == null) {
            return new GcHeapConfiguration(0, 0, 0, 0, 0, false, "");
        }
        return new GcHeapConfiguration(
                readLong(JdkAttributes.HEAP_MIN_SIZE, first),
                readLong(JdkAttributes.HEAP_MAX_SIZE, first),
                readLong(JdkAttributes.HEAP_INITIAL_SIZE, first),
                readLong(JdkAttributes.HEAP_OBJECT_ALIGNMENT, first),
                readLong(JdkAttributes.HEAP_ADDRESS_SIZE, first),
                readBoolean(JdkAttributes.HEAP_USE_COMPRESSED_OOPS, first),
                readString(JdkAttributes.HEAP_COMPRESSED_OOPS_MODE, first));
    }

    // --- 5C: GC Summary ---

    @Override
    public List<GcSummary> loadGcSummaries(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection gcEvents = events.apply(JdkFilters.GARBAGE_COLLECTION);
        Map<String, List<IItem>> byName = new LinkedHashMap<>();
        gcEvents.stream().flatMap(IItemIterable::stream).forEach(item -> {
            String name = readString(JdkAttributes.GC_NAME, item);
            byName.computeIfAbsent(name, k -> new ArrayList<>()).add(item);
        });
        List<GcSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<IItem>> entry : byName.entrySet()) {
            List<IItem> items = entry.getValue();
            long count = items.size();
            List<Long> durations = items.stream()
                    .map(this::readDurationMicros)
                    .sorted()
                    .toList();
            long total = durations.stream().mapToLong(Long::longValue).sum();
            double avg = count > 0 ? (double) total / count : 0;
            long max = durations.isEmpty() ? 0 : durations.getLast();
            summaries.add(new GcSummary(entry.getKey(), count, total / 1000, avg / 1000, max / 1000, total / 1000));
        }
        return List.copyOf(summaries);
    }

    // --- 5D: GC Details ---

    @Override
    public List<GcEvent> loadGcEvents(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection gcEvents = events.apply(JdkFilters.GARBAGE_COLLECTION);
        List<GcEvent> result = new ArrayList<>();
        gcEvents.stream().flatMap(IItemIterable::stream).forEach(item -> {
            result.add(new GcEvent(
                    readLong(JdkAttributes.GC_ID, item),
                    readString(JdkAttributes.GC_NAME, item),
                    readString(JdkAttributes.GC_CAUSE, item),
                    readLongMicros(JdkAttributes.GC_LONGEST_PAUSE, item),
                    readDurationMicros(item),
                    readInstant(JfrAttributes.START_TIME, item)));
        });
        result.sort(Comparator.comparingLong(GcEvent::gcId));
        return List.copyOf(result);
    }

    @Override
    public ChartDefinition loadGcHeapChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        return buildHeapMetaspaceChart(events, JdkFilters.HEAP_SUMMARY_AFTER_GC,
                JdkAttributes.HEAP_USED, JdkAttributes.HEAP_TOTAL, "Time", "Bytes", "Used Heap", "Total Heap");
    }

    @Override
    public ChartDefinition loadGcMetaspaceChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ChartDataPoint> usedPoints = new ArrayList<>();
        List<ChartDataPoint> committedPoints = new ArrayList<>();
        events.apply(JdkFilters.METASPACE_SUMMARY_AFTER_GC).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    usedPoints.add(new ChartDataPoint(time, readDoubleBytes(JdkAttributes.GC_METASPACE_USED, item)));
                    committedPoints.add(new ChartDataPoint(time, readDoubleBytes(JdkAttributes.GC_METASPACE_COMMITTED, item)));
                });
        return new ChartDefinition("Time", "Bytes", List.of(
                new ChartSeries("metaspace-used", "Used Metaspace", ChartSeriesType.LINE, List.copyOf(usedPoints)),
                new ChartSeries("metaspace-committed", "Committed Metaspace", ChartSeriesType.LINE, List.copyOf(committedPoints))));
    }

    @Override
    public ChartDefinition loadGcPauseChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ChartDataPoint> pausePoints = new ArrayList<>();
        events.apply(JdkFilters.GARBAGE_COLLECTION).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    pausePoints.add(new ChartDataPoint(time, readDurationMicros(item) / 1000.0));
                });
        return new ChartDefinition("Time", "Pause (ms)", List.of(
                new ChartSeries("gc-pause", "GC Pause", ChartSeriesType.LINE, List.copyOf(pausePoints))));
    }

    @Override
    public List<GcReferenceStat> loadGcReferenceStats(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection refStats = events.apply(ItemFilters.type(JdkTypeIDs.GC_REFERENCE_STATISTICS));
        List<GcReferenceStat> result = new ArrayList<>();
        refStats.stream().flatMap(IItemIterable::stream).forEach(item -> {
            result.add(new GcReferenceStat(
                    readLong(JdkAttributes.GC_ID, item),
                    readString(JdkAttributes.REFERENCE_STATISTICS_TYPE, item),
                    readLong(JdkAttributes.REFERENCE_STATISTICS_COUNT, item)));
        });
        return List.copyOf(result);
    }

    @Override
    public List<GcHeapSummary> loadGcHeapSummaries(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<GcHeapSummary> result = new ArrayList<>();
        events.apply(JdkFilters.HEAP_SUMMARY_AFTER_GC).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new GcHeapSummary(
                            readLong(JdkAttributes.GC_ID, item),
                            readString(JdkAttributes.GC_WHEN, item),
                            readLongBytes(JdkAttributes.HEAP_USED, item),
                            readLongBytes(JdkAttributes.HEAP_TOTAL, item),
                            readLongBytes(JdkAttributes.GC_METASPACE_USED, item),
                            readLongBytes(JdkAttributes.GC_METASPACE_COMMITTED, item),
                            readLongBytes(JdkAttributes.GC_METASPACE_RESERVED, item)));
                });
        return List.copyOf(result);
    }

    // --- 5E: Compilations ---

    @Override
    public List<CompilationEvent> loadCompilationEvents(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<CompilationEvent> result = new ArrayList<>();
        events.apply(JdkFilters.COMPILATION).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new CompilationEvent(
                            readLong(JdkAttributes.COMPILER_COMPILATION_ID, item),
                            readString(JdkAttributes.COMPILER_METHOD_STRING, item),
                            readBoolean(JdkAttributes.COMPILER_COMPILATION_SUCCEEDED, item),
                            readDurationMicros(item),
                            readLong(JdkAttributes.COMPILER_CODE_SIZE, item),
                            readLong(JdkAttributes.COMPILER_INLINED_SIZE, item),
                            readInstant(JfrAttributes.START_TIME, item)));
                });
        return List.copyOf(result);
    }

    @Override
    public List<CompilationEvent> loadCompilationFailures(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<CompilationEvent> result = new ArrayList<>();
        events.apply(JdkFilters.COMPILER_FAILURE).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new CompilationEvent(
                            readLong(JdkAttributes.COMPILER_COMPILATION_ID, item),
                            readString(JdkAttributes.COMPILER_METHOD_STRING, item),
                            false,
                            readDurationMicros(item),
                            0,
                            0,
                            readInstant(JfrAttributes.START_TIME, item)));
                });
        return List.copyOf(result);
    }

    @Override
    public ChartDefinition loadCompilationDurationChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ChartDataPoint> points = new ArrayList<>();
        events.apply(JdkFilters.COMPILATION).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    points.add(new ChartDataPoint(time, readDurationMicros(item) / 1000.0));
                });
        return new ChartDefinition("Time", "Duration (ms)", List.of(
                new ChartSeries("compilation-duration", "Compilation Duration", ChartSeriesType.LINE, List.copyOf(points))));
    }

    // --- 5F: Code Cache ---

    @Override
    public List<CodeCacheSweep> loadCodeCacheSweeps(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<CodeCacheSweep> result = new ArrayList<>();
        events.apply(JdkFilters.SWEEP_CODE_CACHE).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new CodeCacheSweep(
                            readInstant(JfrAttributes.START_TIME, item),
                            readLong(JdkAttributes.SWEEP_INDEX, item),
                            readDurationMicros(item),
                            readLong(JdkAttributes.SWEEP_METHOD_FLUSHED, item),
                            readLong(JdkAttributes.SWEEP_METHOD_SWEPT, item),
                            readLong(JdkAttributes.SWEEP_METHOD_RECLAIMED, item)));
                });
        return List.copyOf(result);
    }

    @Override
    public List<CodeCacheStats> loadCodeCacheStatistics(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<CodeCacheStats> result = new ArrayList<>();
        events.apply(JdkFilters.CODE_CACHE_STATISTICS).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new CodeCacheStats(
                            readInstant(JfrAttributes.START_TIME, item),
                            readString(JdkAttributes.CODE_HEAP, item),
                            readLong(JdkAttributes.ENTRIES, item),
                            readLong(JdkAttributes.METHODS, item),
                            0,
                            0));
                });
        return List.copyOf(result);
    }

    @Override
    public ChartDefinition loadCodeCacheEntriesChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ChartDataPoint> entriesPoints = new ArrayList<>();
        List<ChartDataPoint> methodsPoints = new ArrayList<>();
        events.apply(JdkFilters.CODE_CACHE_STATISTICS).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    entriesPoints.add(new ChartDataPoint(time, readLong(JdkAttributes.ENTRIES, item)));
                    methodsPoints.add(new ChartDataPoint(time, readLong(JdkAttributes.METHODS, item)));
                });
        return new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("entries", "Entries", ChartSeriesType.LINE, List.copyOf(entriesPoints)),
                new ChartSeries("methods", "Methods", ChartSeriesType.LINE, List.copyOf(methodsPoints))));
    }

    @Override
    public ChartDefinition loadCodeCacheSweepChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ChartDataPoint> sweptPoints = new ArrayList<>();
        List<ChartDataPoint> flushedPoints = new ArrayList<>();
        events.apply(JdkFilters.SWEEP_CODE_CACHE).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    sweptPoints.add(new ChartDataPoint(time, readLong(JdkAttributes.SWEEP_METHOD_SWEPT, item)));
                    flushedPoints.add(new ChartDataPoint(time, readLong(JdkAttributes.SWEEP_METHOD_FLUSHED, item)));
                });
        return new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("swept", "Swept", ChartSeriesType.LINE, List.copyOf(sweptPoints)),
                new ChartSeries("flushed", "Flushed", ChartSeriesType.LINE, List.copyOf(flushedPoints))));
    }

    // --- 5G: Class Loading ---

    @Override
    public List<ClassloaderSummary> loadClassloaderHistogram(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection loadStats = events.apply(JdkFilters.CLASS_LOAD_STATISTICS);
        Map<String, long[]> byLoader = new LinkedHashMap<>();
        loadStats.stream().flatMap(IItemIterable::stream).forEach(item -> {
            String loader = readString(JdkAttributes.CLASSLOADER, item);
            long[] counts = byLoader.computeIfAbsent(loader, k -> new long[2]);
            counts[0] += readLong(JdkAttributes.CLASSLOADER_LOADED_COUNT, item);
            counts[1] += readLong(JdkAttributes.CLASSLOADER_UNLOADED_COUNT, item);
        });
        List<ClassloaderSummary> result = new ArrayList<>();
        byLoader.forEach((loader, counts) -> {
            String display = (loader == null || loader.isEmpty()) ? "bootstrap" : loader;
            result.add(new ClassloaderSummary(display, counts[0], counts[1]));
        });
        result.sort(Comparator.comparingLong(ClassloaderSummary::loadedCount).reversed());
        return List.copyOf(result);
    }

    @Override
    public List<ClassloadEvent> loadClassloadEvents(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ClassloadEvent> result = new ArrayList<>();
        events.apply(JdkFilters.CLASS_LOAD).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new ClassloadEvent("load",
                            readInstant(JfrAttributes.START_TIME, item),
                            readString(JdkAttributes.CLASS_LOADED, item),
                            readString(JdkAttributes.CLASS_DEFINING_CLASSLOADER_STRING, item),
                            readString(JdkAttributes.CLASS_INITIATING_CLASSLOADER_STRING, item),
                            readDurationMicros(item)));
                });
        events.apply(JdkFilters.CLASS_UNLOAD).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new ClassloadEvent("unload",
                            readInstant(JfrAttributes.START_TIME, item),
                            readString(JdkAttributes.CLASS_UNLOADED, item),
                            readString(JdkAttributes.CLASS_DEFINING_CLASSLOADER_STRING, item),
                            "", 0));
                });
        events.apply(JdkFilters.CLASS_DEFINE).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new ClassloadEvent("define",
                            readInstant(JfrAttributes.START_TIME, item),
                            readString(JdkAttributes.CLASS_LOADED, item),
                            readString(JdkAttributes.CLASS_DEFINING_CLASSLOADER_STRING, item),
                            readString(JdkAttributes.CLASS_INITIATING_CLASSLOADER_STRING, item),
                            readDurationMicros(item)));
                });
        result.sort(Comparator.comparing(ClassloadEvent::startTime));
        return List.copyOf(result);
    }

    @Override
    public List<ClassloaderStatistics> loadClassloaderStatistics(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ClassloaderStatistics> result = new ArrayList<>();
        events.apply(JdkFilters.CLASS_LOADER_STATISTICS).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new ClassloaderStatistics(
                            readString(JdkAttributes.CLASSLOADER, item),
                            readString(JdkAttributes.PARENT_CLASSLOADER, item),
                            readLong(JdkAttributes.CLASSLOADER_LOADED_COUNT, item),
                            readLong(JdkAttributes.ANONYMOUS_CHUNK_SIZE, item),
                            readLong(JdkAttributes.ANONYMOUS_BLOCK_SIZE, item),
                            readLong(JdkAttributes.ANONYMOUS_CLASS_COUNT, item)));
                });
        return List.copyOf(result);
    }

    @Override
    public ChartDefinition loadClassLoadingChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<ChartDataPoint> loadedPoints = new ArrayList<>();
        List<ChartDataPoint> unloadedPoints = new ArrayList<>();
        events.apply(JdkFilters.CLASS_LOAD_STATISTICS).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    loadedPoints.add(new ChartDataPoint(time, readLong(JdkAttributes.CLASSLOADER_LOADED_COUNT, item)));
                    unloadedPoints.add(new ChartDataPoint(time, readLong(JdkAttributes.CLASSLOADER_UNLOADED_COUNT, item)));
                });
        return new ChartDefinition("Time", "Count", List.of(
                new ChartSeries("loaded", "Loaded", ChartSeriesType.LINE, List.copyOf(loadedPoints)),
                new ChartSeries("unloaded", "Unloaded", ChartSeriesType.LINE, List.copyOf(unloadedPoints))));
    }

    // --- 5H: VM Operations ---

    @Override
    public List<VmOperationSummary> loadVmOperationSummary(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        Map<String, long[]> byOperation = new LinkedHashMap<>();
        events.apply(JdkFilters.VM_OPERATIONS).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    String op = readString(JdkAttributes.OPERATION, item);
                    long[] stats = byOperation.computeIfAbsent(op, k -> new long[3]);
                    long duration = readDurationMicros(item);
                    stats[0]++;
                    stats[1] += duration;
                    if (duration > stats[2]) {
                        stats[2] = duration;
                    }
                });
        List<VmOperationSummary> result = new ArrayList<>();
        byOperation.forEach((op, stats) -> {
            result.add(new VmOperationSummary(op, stats[0], stats[1], stats[2]));
        });
        result.sort(Comparator.comparingLong(VmOperationSummary::totalDurationMicros).reversed());
        return List.copyOf(result);
    }

    @Override
    public List<VmOperationEvent> loadVmOperationEvents(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        List<VmOperationEvent> result = new ArrayList<>();
        events.apply(JdkFilters.VM_OPERATIONS).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    result.add(new VmOperationEvent(
                            readInstant(JfrAttributes.START_TIME, item),
                            readString(JdkAttributes.OPERATION, item),
                            readBoolean(JdkAttributes.BLOCKING, item),
                            readBoolean(JdkAttributes.SAFEPOINT, item),
                            readDurationMicros(item),
                            readEventThreadName(item)));
                });
        result.sort(Comparator.comparing(VmOperationEvent::startTime));
        return List.copyOf(result);
    }

    // --- Internal helpers ---

    private IItemCollection loadEvents(RecordingSummary recording) {
        try {
            return JfrLoaderToolkit.loadEvents(recording.path().toFile());
        } catch (IOException | CouldNotLoadRecordingException e) {
            throw new JmcFxException("Unable to load recording for JVM internals: " + recording.path(), e);
        }
    }

    private IItem firstItem(IItemCollection events) {
        return events.stream()
                .flatMap(IItemIterable::stream)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private <T> T read(IAttribute<T> attribute, IItem item) {
        IMemberAccessor<T, IItem> accessor =
                (IMemberAccessor<T, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    private String readString(IAttribute<?> attribute, IItem item) {
        Object value = read(attribute, item);
        if (value != null) {
            return value.toString();
        }
        // Fallback: try raw accessor
        Object raw = readRaw(attribute, item);
        return raw == null ? "" : raw.toString();
    }

    private long readLong(IAttribute<?> attribute, IItem item) {
        Object value = readRaw(attribute, item);
        if (value instanceof IQuantity quantity) {
            return quantity.clampedLongValueIn(UnitLookup.NUMBER_UNITY);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private boolean readBoolean(IAttribute<Boolean> attribute, IItem item) {
        Boolean value = read(attribute, item);
        return value != null && value;
    }

    private Instant readInstant(IAttribute<IQuantity> attribute, IItem item) {
        IQuantity quantity = read(attribute, item);
        if (quantity == null) {
            return Instant.EPOCH;
        }
        return UnitLookup.toDate(quantity).toInstant();
    }

    private long readDurationMicros(IItem item) {
        IQuantity duration = read(JfrAttributes.DURATION, item);
        return duration == null ? 0 : duration.clampedLongValueIn(UnitLookup.MICROSECOND);
    }

    private long readLongMicros(IAttribute<IQuantity> attribute, IItem item) {
        IQuantity value = read(attribute, item);
        return value == null ? 0 : value.clampedLongValueIn(UnitLookup.MICROSECOND);
    }

    private long readLongBytes(IAttribute<IQuantity> attribute, IItem item) {
        IQuantity value = read(attribute, item);
        return value == null ? 0 : value.clampedLongValueIn(UnitLookup.BYTE);
    }

    private double readDoubleBytes(IAttribute<IQuantity> attribute, IItem item) {
        IQuantity value = read(attribute, item);
        return value == null ? 0 : value.doubleValueIn(UnitLookup.BYTE);
    }

    private double readEpochSeconds(IAttribute<IQuantity> attribute, IItem item) {
        IQuantity value = read(attribute, item);
        if (value == null) {
            return 0;
        }
        return value.clampedLongValueIn(UnitLookup.EPOCH_MS) / 1000.0;
    }

    @SuppressWarnings("unchecked")
    private Object readRaw(IAttribute<?> attribute, IItem item) {
        IMemberAccessor<Object, IItem> accessor =
                (IMemberAccessor<Object, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    private String readFlagValue(IItem item) {
        Boolean boolVal = read(JdkAttributes.FLAG_VALUE_BOOLEAN, item);
        if (boolVal != null) {
            return boolVal.toString();
        }
        IQuantity numVal = read(JdkAttributes.FLAG_VALUE_NUMBER, item);
        if (numVal != null) {
            return numVal.displayUsing(org.openjdk.jmc.common.IDisplayable.AUTO);
        }
        return readString(JdkAttributes.FLAG_VALUE_TEXT, item);
    }

    private String readOldFlagValue(IItem item) {
        Boolean boolVal = read(JdkAttributes.FLAG_OLD_VALUE_BOOLEAN, item);
        if (boolVal != null) {
            return boolVal.toString();
        }
        IQuantity numVal = read(JdkAttributes.FLAG_OLD_VALUE_NUMBER, item);
        if (numVal != null) {
            return numVal.displayUsing(org.openjdk.jmc.common.IDisplayable.AUTO);
        }
        return readString(JdkAttributes.FLAG_OLD_VALUE_TEXT, item);
    }

    private String readNewFlagValue(IItem item) {
        Boolean boolVal = read(JdkAttributes.FLAG_NEW_VALUE_BOOLEAN, item);
        if (boolVal != null) {
            return boolVal.toString();
        }
        IQuantity numVal = read(JdkAttributes.FLAG_NEW_VALUE_NUMBER, item);
        if (numVal != null) {
            return numVal.displayUsing(org.openjdk.jmc.common.IDisplayable.AUTO);
        }
        return readString(JdkAttributes.FLAG_NEW_VALUE_TEXT, item);
    }

    private String readEventThreadName(IItem item) {
        String name = readString(JdkAttributes.EVENT_THREAD_NAME, item);
        if (!name.isEmpty()) {
            return name;
        }
        var thread = read(JfrAttributes.EVENT_THREAD, item);
        if (thread instanceof org.openjdk.jmc.common.IMCThread mcThread) {
            String threadName = mcThread.getThreadName();
            return threadName == null ? "" : threadName;
        }
        return "";
    }

    private ChartDefinition buildHeapMetaspaceChart(IItemCollection events,
            org.openjdk.jmc.common.item.IItemFilter filter,
            IAttribute<IQuantity> usedAttr, IAttribute<IQuantity> totalAttr,
            String xLabel, String yLabel, String usedLabel, String totalLabel) {
        List<ChartDataPoint> usedPoints = new ArrayList<>();
        List<ChartDataPoint> totalPoints = new ArrayList<>();
        events.apply(filter).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> {
                    double time = readEpochSeconds(JfrAttributes.START_TIME, item);
                    usedPoints.add(new ChartDataPoint(time, readDoubleBytes(usedAttr, item)));
                    totalPoints.add(new ChartDataPoint(time, readDoubleBytes(totalAttr, item)));
                });
        return new ChartDefinition(xLabel, yLabel, List.of(
                new ChartSeries(usedLabel.toLowerCase().replace(' ', '-'), usedLabel, ChartSeriesType.LINE, List.copyOf(usedPoints)),
                new ChartSeries(totalLabel.toLowerCase().replace(' ', '-'), totalLabel, ChartSeriesType.LINE, List.copyOf(totalPoints))));
    }
}
