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
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;

import com.youngledo.jmcfx.domain.model.ChartDataPoint;
import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;
import com.youngledo.jmcfx.domain.model.ChartSeriesType;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.HeapService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

public class JmcHeapService implements HeapService {

    @Override
    public List<HeapClassHistogram> loadHeapClassHistogram(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection objectCounts = events.apply(JdkFilters.OBJECT_COUNT);
        if (!objectCounts.hasItems()) {
            return List.of();
        }

        Map<String, ClassHistogramAccumulator> buckets = new HashMap<>();
        for (IItemIterable itemIter : objectCounts) {
            IMemberAccessor<String, IItem> classAccessor =
                    getAccessor(itemIter, JdkAttributes.OBJECT_CLASS_FULLNAME);
            IMemberAccessor<IQuantity, IItem> countAccessor =
                    getAccessor(itemIter, JdkAttributes.COUNT);
            for (IItem item : itemIter) {
                String className = classAccessor != null ? classAccessor.getMember(item) : "<unknown>";
                long count = 0;
                if (countAccessor != null) {
                    IQuantity qty = countAccessor.getMember(item);
                    if (qty != null) {
                        count = qty.longValue();
                    }
                }
                buckets.computeIfAbsent(className,
                        k -> new ClassHistogramAccumulator(className)).add(count);
            }
        }

        long totalInstances = buckets.values().stream().mapToLong(a -> a.instances).sum();

        List<HeapClassHistogram> results = new ArrayList<>();
        for (ClassHistogramAccumulator acc : buckets.values()) {
            double pct = totalInstances > 0 ? (acc.instances * 100.0) / totalInstances : 0;
            results.add(new HeapClassHistogram(acc.className, acc.instances, 0, 0, pct));
        }
        results.sort(Comparator.comparingLong(HeapClassHistogram::instances).reversed());
        return List.copyOf(results);
    }

    @Override
    public ChartDefinition loadHeapUsageTimeline(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection heapSummaries = events.apply(JdkFilters.HEAP_SUMMARY);
        if (!heapSummaries.hasItems()) {
            return new ChartDefinition("Time", "Bytes", List.of());
        }

        List<ChartDataPoint> usedPoints = new ArrayList<>();
        List<ChartDataPoint> totalPoints = new ArrayList<>();
        for (IItemIterable itemIter : heapSummaries) {
            @SuppressWarnings("unchecked")
            IMemberAccessor<IQuantity, IItem> startTimeAccessor =
                    (IMemberAccessor<IQuantity, IItem>) JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> heapUsedAccessor =
                    getAccessor(itemIter, JdkAttributes.HEAP_USED);
            IMemberAccessor<IQuantity, IItem> heapTotalAccessor =
                    getAccessor(itemIter, JdkAttributes.HEAP_TOTAL);
            for (IItem item : itemIter) {
                IQuantity startTime = startTimeAccessor != null ? startTimeAccessor.getMember(item) : null;
                if (startTime == null) {
                    continue;
                }
                Instant instant = UnitLookup.toDate(startTime).toInstant();
                double timeMs = instant.toEpochMilli();

                long usedBytes = 0;
                if (heapUsedAccessor != null) {
                    IQuantity used = heapUsedAccessor.getMember(item);
                    if (used != null) {
                        usedBytes = used.longValue();
                    }
                }
                long totalBytes = 0;
                if (heapTotalAccessor != null) {
                    IQuantity total = heapTotalAccessor.getMember(item);
                    if (total != null) {
                        totalBytes = total.longValue();
                    }
                }
                usedPoints.add(new ChartDataPoint(timeMs, usedBytes));
                totalPoints.add(new ChartDataPoint(timeMs, totalBytes));
            }
        }
        usedPoints.sort(Comparator.comparingDouble(ChartDataPoint::x));
        totalPoints.sort(Comparator.comparingDouble(ChartDataPoint::x));

        ChartSeries usedSeries = new ChartSeries("heapUsed", "Used Heap",
                ChartSeriesType.AREA, List.copyOf(usedPoints));
        ChartSeries totalSeries = new ChartSeries("heapTotal", "Total Heap",
                ChartSeriesType.LINE, List.copyOf(totalPoints));
        return new ChartDefinition("Time", "Bytes", List.of(usedSeries, totalSeries));
    }

    @SuppressWarnings("unchecked")
    private static <T> IMemberAccessor<T, IItem> getAccessor(
            IItemIterable itemIterable, IAttribute<T> attribute) {
        return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
    }

    private IItemCollection loadEvents(RecordingSummary recording) {
        try {
            return JfrLoaderToolkit.loadEvents(recording.path().toFile());
        } catch (IOException | CouldNotLoadRecordingException e) {
            throw new JmcFxException("Unable to load recording for heap analysis: " + recording.path(), e);
        }
    }

    private static final class ClassHistogramAccumulator {
        final String className;
        long instances;

        ClassHistogramAccumulator(String className) {
            this.className = className;
        }

        void add(long count) {
            instances += count;
        }
    }
}
