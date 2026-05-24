package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.domain.service.JavaAppService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

/// JMC-backed Java Application service adapter.
///
/// Uses JMC core APIs to extract thread histogram, certificate, native library,
/// and thread dump data from flight recordings.
public class JmcJavaAppService implements JavaAppService {

    @Override
    public List<ThreadHistogramRow> loadThreadHistogram(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);

        Map<String, ThreadAccumulator> buckets = new HashMap<>();

        // Profiling counts from execution samples
        IItemCollection samples = events.apply(JdkFilters.EXECUTION_SAMPLE);
        for (IItemIterable itemIter : samples) {
            IMemberAccessor<String, IItem> nameAccessor =
                    JdkAttributes.EVENT_THREAD_NAME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String name = nameAccessor != null ? nameAccessor.getMember(item) : null;
                if (name != null) {
                    buckets.computeIfAbsent(name, k -> new ThreadAccumulator(name)).profilingCount++;
                }
            }
        }

        // IO and blocked durations from thread latencies
        IItemCollection latencies = events.apply(JdkFilters.THREAD_LATENCIES);
        for (IItemIterable itemIter : latencies) {
            for (IItem item : itemIter) {
                IMemberAccessor<String, IItem> nameAccessor =
                        JdkAttributes.EVENT_THREAD_NAME.getAccessor(itemIter.getType());
                String name = nameAccessor != null ? nameAccessor.getMember(item) : null;
                if (name == null) {
                    continue;
                }
                ThreadAccumulator acc = buckets.computeIfAbsent(name, k -> new ThreadAccumulator(name));
                String typeId = itemIter.getType().getIdentifier();
                long durationMs = getDurationMillis(item, itemIter);

                if (isIoType(typeId)) {
                    acc.ioDurationMillis += durationMs;
                }
                if (JdkTypeIDs.MONITOR_ENTER.equals(typeId)) {
                    acc.blockedDurationMillis += durationMs;
                }
            }
        }

        // Allocation from jdk.ObjectAllocationInNewTLAB and jdk.ObjectAllocationOutsideTLAB
        IItemCollection allocInTlab = events.apply(JdkFilters.ALLOC_INSIDE_TLAB);
        IItemCollection allocOutsideTlab = events.apply(JdkFilters.ALLOC_OUTSIDE_TLAB);
        accumulateAllocation(buckets, allocInTlab);
        accumulateAllocation(buckets, allocOutsideTlab);

        // Exceptions from throwables
        IItemCollection throwables = events.apply(JdkFilters.THROWABLES);
        for (IItemIterable itemIter : throwables) {
            IMemberAccessor<String, IItem> nameAccessor =
                    JdkAttributes.EVENT_THREAD_NAME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String name = nameAccessor != null ? nameAccessor.getMember(item) : null;
                if (name != null) {
                    buckets.computeIfAbsent(name, k -> new ThreadAccumulator(name)).exceptionCount++;
                }
            }
        }

        List<ThreadHistogramRow> results = new ArrayList<>();
        for (ThreadAccumulator acc : buckets.values()) {
            results.add(new ThreadHistogramRow(acc.name, acc.profilingCount,
                    acc.ioDurationMillis, acc.blockedDurationMillis,
                    acc.allocatedBytes, acc.exceptionCount));
        }
        results.sort(Comparator.comparingInt(ThreadHistogramRow::profilingCount).reversed());
        return JmcResultLimiter.limitRows(results);
    }

    @Override
    public ChartDefinition loadOverviewChart(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);

        // Build profiling samples timeline
        List<ChartDataPoint> profilingPoints = buildCountTimelinePoints(
                events.apply(JdkFilters.EXECUTION_SAMPLE));

        // Build blocked duration timeline
        List<ChartDataPoint> blockedPoints = buildDurationTimelinePoints(
                events.apply(ItemFilters.type(JdkTypeIDs.MONITOR_ENTER)));

        // Build IO duration timeline
        IItemCollection ioEvents = events.apply(ItemFilters.or(
                ItemFilters.type(JdkTypeIDs.SOCKET_READ),
                ItemFilters.type(JdkTypeIDs.SOCKET_WRITE),
                ItemFilters.type(JdkTypeIDs.FILE_READ),
                ItemFilters.type(JdkTypeIDs.FILE_WRITE)));
        List<ChartDataPoint> ioPoints = buildDurationTimelinePoints(ioEvents);

        // Build exception timeline
        List<ChartDataPoint> exceptionPoints = new ArrayList<>();
        IItemCollection throwables = events.apply(JdkFilters.THROWABLES);
        Map<Long, Integer> countByWindow = new HashMap<>();
        for (IItemIterable itemIter : throwables) {
            IMemberAccessor<IQuantity, IItem> startAccessor =
                    JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                IQuantity start = startAccessor != null ? startAccessor.getMember(item) : null;
                if (start != null) {
                    long windowMs = toEpochMs(start) / 1000 * 1000;
                    countByWindow.merge(windowMs, 1, Integer::sum);
                }
            }
        }
        countByWindow.entrySet().stream()
                .map(e -> new ChartDataPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ChartDataPoint::x))
                .forEach(exceptionPoints::add);

        List<ChartSeries> series = new ArrayList<>();
        if (!profilingPoints.isEmpty()) {
            series.add(new ChartSeries("profiling", "Profiling Samples",
                    ChartSeriesType.LINE, List.copyOf(profilingPoints)));
        }
        if (!ioPoints.isEmpty()) {
            series.add(new ChartSeries("io", "IO Duration (ms)",
                    ChartSeriesType.LINE, List.copyOf(ioPoints)));
        }
        if (!blockedPoints.isEmpty()) {
            series.add(new ChartSeries("blocked", "Blocked Duration (ms)",
                    ChartSeriesType.LINE, List.copyOf(blockedPoints)));
        }
        if (!exceptionPoints.isEmpty()) {
            series.add(new ChartSeries("exceptions", "Exceptions",
                    ChartSeriesType.LINE, List.copyOf(exceptionPoints)));
        }

        return JmcResultLimiter.limitChart(new ChartDefinition("Time", "Value", List.copyOf(series)));
    }

    @Override
    public List<X509CertificateEntry> loadCertificates(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        // jdk.X509Certificate may not have a JdkTypeIDs constant in all JMC versions
        IItemCollection certEvents = events.apply(ItemFilters.type("jdk.X509Certificate"));
        if (!certEvents.hasItems()) {
            return List.of();
        }

        List<X509CertificateEntry> results = new ArrayList<>();
        for (IItemIterable itemIter : certEvents) {
            for (IItem item : itemIter) {
                Instant startTime = getStartTime(item, itemIter);
                // Certificate attributes may not be available as JdkAttributes constants in JMC 9.x
                String certId = firstNonBlank(
                        getStringById(item, itemIter, "certificateId"),
                        "");
                String algorithm = firstNonBlank(
                        getStringById(item, itemIter, "signatureAlgorithm"),
                        getStringById(item, itemIter, "algorithm"),
                        "");
                String subject = firstNonBlank(
                        getStringById(item, itemIter, "subject"),
                        "");
                String issuer = firstNonBlank(
                        getStringById(item, itemIter, "issuer"),
                        "");
                String serial = firstNonBlank(
                        getStringById(item, itemIter, "serialNumber"),
                        getStringById(item, itemIter, "serial"),
                        "");
                Instant validFrom = getInstantById(item, itemIter, "validFrom");
                Instant validTo = getInstantById(item, itemIter, "validTo");
                int keyLength = getIntById(item, itemIter, "keyLength");

                results.add(new X509CertificateEntry(startTime, certId, algorithm,
                        subject, issuer, serial, validFrom, validTo, keyLength));
            }
        }
        results.sort(Comparator.comparing(X509CertificateEntry::startTime));
        return JmcResultLimiter.limitRows(results);
    }

    @Override
    public List<NativeLibraryEntry> loadNativeLibraries(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection libEvents = events.apply(ItemFilters.type(JdkTypeIDs.NATIVE_LIBRARY));
        if (!libEvents.hasItems()) {
            return List.of();
        }

        List<NativeLibraryEntry> results = new ArrayList<>();
        for (IItemIterable itemIter : libEvents) {
            for (IItem item : itemIter) {
                Instant startTime = getStartTime(item, itemIter);
                String name = getStringValue(item, itemIter, JdkAttributes.NATIVE_LIBRARY_NAME);
                String basePath = firstNonBlank(
                        getStringById(item, itemIter, "basePath"),
                        "");
                String absolutePath = firstNonBlank(
                        getStringById(item, itemIter, "absolutePath"),
                        getStringById(item, itemIter, "path"),
                        "");

                results.add(new NativeLibraryEntry(startTime,
                        name != null ? name : "",
                        basePath, absolutePath));
            }
        }
        results.sort(Comparator.comparing(NativeLibraryEntry::startTime));
        return JmcResultLimiter.limitRows(results);
    }

    @Override
    public List<ThreadDumpEntry> loadThreadDumps(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection dumpEvents = events.apply(JdkFilters.THREAD_DUMP);
        if (!dumpEvents.hasItems()) {
            return List.of();
        }

        List<ThreadDumpEntry> results = new ArrayList<>();
        for (IItemIterable itemIter : dumpEvents) {
            for (IItem item : itemIter) {
                Instant startTime = getStartTime(item, itemIter);
                String dumpText = "";
                // The jdk.ThreadDump event stores the full dump text as a string attribute
                for (IAttribute<?> attr : itemIter.getType().getAttributes()) {
                    String attrId = attr.getIdentifier().toLowerCase();
                    if (attrId.contains("result") || attrId.contains("dump") || attrId.contains("text")
                            || attrId.contains("output")) {
                        Object value = readRaw(attr, item);
                        if (value instanceof String text && !text.isBlank()) {
                            dumpText = text;
                            break;
                        }
                    }
                }
                results.add(new ThreadDumpEntry(startTime, dumpText));
            }
        }
        results.sort(Comparator.comparing(ThreadDumpEntry::startTime));
        return JmcResultLimiter.limitRows(results);
    }

    // --- Private helpers ---

    private void accumulateAllocation(Map<String, ThreadAccumulator> buckets, IItemCollection allocEvents) {
        for (IItemIterable itemIter : allocEvents) {
            IMemberAccessor<String, IItem> nameAccessor =
                    JdkAttributes.EVENT_THREAD_NAME.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> allocSizeAccessor =
                    getAccessor(itemIter, JdkAttributes.ALLOCATION_SIZE);
            for (IItem item : itemIter) {
                String name = nameAccessor != null ? nameAccessor.getMember(item) : null;
                if (name == null) {
                    continue;
                }
                long allocSize = 0;
                if (allocSizeAccessor != null) {
                    IQuantity qty = allocSizeAccessor.getMember(item);
                    if (qty != null) {
                        allocSize = qty.longValue();
                    }
                }
                if (allocSize > 0) {
                    buckets.computeIfAbsent(name, k -> new ThreadAccumulator(name))
                            .allocatedBytes += allocSize;
                }
            }
        }
    }

    private List<ChartDataPoint> buildCountTimelinePoints(IItemCollection filtered) {
        Map<Long, Integer> countByWindow = new HashMap<>();
        for (IItemIterable itemIter : filtered) {
            IMemberAccessor<IQuantity, IItem> startAccessor =
                    JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                IQuantity start = startAccessor != null ? startAccessor.getMember(item) : null;
                if (start != null) {
                    long windowMs = toEpochMs(start) / 1000 * 1000;
                    countByWindow.merge(windowMs, 1, Integer::sum);
                }
            }
        }
        return countByWindow.entrySet().stream()
                .map(e -> new ChartDataPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ChartDataPoint::x))
                .toList();
    }

    private List<ChartDataPoint> buildDurationTimelinePoints(IItemCollection filtered) {
        Map<Long, Long> durationByWindow = new HashMap<>();
        for (IItemIterable itemIter : filtered) {
            IMemberAccessor<IQuantity, IItem> startAccessor =
                    JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                IQuantity start = startAccessor != null ? startAccessor.getMember(item) : null;
                long durationMs = getDurationMillis(item, itemIter);
                if (start != null) {
                    long windowMs = toEpochMs(start) / 1000 * 1000;
                    durationByWindow.merge(windowMs, durationMs, Long::sum);
                }
            }
        }
        return durationByWindow.entrySet().stream()
                .map(e -> new ChartDataPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(ChartDataPoint::x))
                .toList();
    }

    private boolean isIoType(String typeId) {
        return JdkTypeIDs.SOCKET_READ.equals(typeId)
                || JdkTypeIDs.SOCKET_WRITE.equals(typeId)
                || JdkTypeIDs.FILE_READ.equals(typeId)
                || JdkTypeIDs.FILE_WRITE.equals(typeId);
    }

    private static long toEpochMs(IQuantity quantity) {
        if (quantity == null) {
            return 0;
        }
        try {
            return quantity.longValueIn(UnitLookup.EPOCH_MS);
        } catch (org.openjdk.jmc.common.unit.QuantityConversionException e) {
            return 0;
        }
    }

    private Instant getStartTime(IItem item, IItemIterable itemIter) {
        IMemberAccessor<IQuantity, IItem> accessor =
                JfrAttributes.START_TIME.getAccessor(itemIter.getType());
        IQuantity time = accessor != null ? accessor.getMember(item) : null;
        return time != null ? UnitLookup.toDate(time).toInstant() : Instant.EPOCH;
    }

    private static long getDurationMillis(IItem item, IItemIterable itemIter) {
        @SuppressWarnings("unchecked")
        IMemberAccessor<IQuantity, IItem> accessor =
                (IMemberAccessor<IQuantity, IItem>) ((IAttribute<IQuantity>) JfrAttributes.DURATION)
                        .getAccessor(itemIter.getType());
        IQuantity duration = accessor != null ? accessor.getMember(item) : null;
        if (duration == null) {
            return 0;
        }
        try {
            return duration.longValueIn(UnitLookup.MILLISECOND);
        } catch (org.openjdk.jmc.common.unit.QuantityConversionException e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> IMemberAccessor<T, IItem> getAccessor(IItemIterable itemIter, IAttribute<T> attribute) {
        return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIter.getType());
    }

    private static String getStringValue(IItem item, IItemIterable itemIter, IAttribute<String> attribute) {
        IMemberAccessor<String, IItem> accessor = getAccessor(itemIter, attribute);
        return accessor != null ? accessor.getMember(item) : null;
    }

    private static int getIntById(IItem item, IItemIterable itemIter, String attributeId) {
        for (IAttribute<?> attr : itemIter.getType().getAttributes()) {
            if (attributeId.equals(attr.getIdentifier())) {
                @SuppressWarnings("unchecked")
                IMemberAccessor<Object, IItem> accessor =
                        (IMemberAccessor<Object, IItem>) attr.getAccessor(itemIter.getType());
                Object value = accessor != null ? accessor.getMember(item) : null;
                if (value instanceof Number number) {
                    return number.intValue();
                }
                break;
            }
        }
        return 0;
    }

    private static Instant getInstantById(IItem item, IItemIterable itemIter, String attributeId) {
        for (IAttribute<?> attr : itemIter.getType().getAttributes()) {
            if (attributeId.equals(attr.getIdentifier())) {
                @SuppressWarnings("unchecked")
                IMemberAccessor<Object, IItem> accessor =
                        (IMemberAccessor<Object, IItem>) attr.getAccessor(itemIter.getType());
                Object value = accessor != null ? accessor.getMember(item) : null;
                if (value instanceof IQuantity qty) {
                    return UnitLookup.toDate(qty).toInstant();
                }
                break;
            }
        }
        return null;
    }

    private static Instant getInstantValue(IItem item, IItemIterable itemIter, IAttribute<IQuantity> attribute) {
        IMemberAccessor<IQuantity, IItem> accessor = getAccessor(itemIter, attribute);
        if (accessor == null) {
            return null;
        }
        IQuantity value = accessor.getMember(item);
        return value != null ? UnitLookup.toDate(value).toInstant() : null;
    }

    @SuppressWarnings("unchecked")
    private static String getStringById(IItem item, IItemIterable itemIter, String attributeId) {
        for (IAttribute<?> attr : itemIter.getType().getAttributes()) {
            if (attributeId.equals(attr.getIdentifier())) {
                IMemberAccessor<Object, IItem> accessor =
                        (IMemberAccessor<Object, IItem>) attr.getAccessor(itemIter.getType());
                Object value = accessor != null ? accessor.getMember(item) : null;
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object readRaw(IAttribute<?> attribute, IItem item) {
        IMemberAccessor<Object, IItem> accessor =
                (IMemberAccessor<Object, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor != null ? accessor.getMember(item) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

    private static final class ThreadAccumulator {
        final String name;
        int profilingCount;
        long ioDurationMillis;
        long blockedDurationMillis;
        long allocatedBytes;
        int exceptionCount;

        ThreadAccumulator(String name) {
            this.name = name;
        }
    }
}
