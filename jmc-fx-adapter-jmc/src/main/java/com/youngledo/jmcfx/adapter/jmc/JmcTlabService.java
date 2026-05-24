package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.TlabService;

public class JmcTlabService implements TlabService {

    private static final long TIMELINE_BUCKET_MILLIS = 1_000;

    @Override
    public List<TlabAllocation> loadTlabAllocations(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection insideTlab = events.apply(JdkFilters.ALLOC_INSIDE_TLAB);
        IItemCollection outsideTlab = events.apply(JdkFilters.ALLOC_OUTSIDE_TLAB);

        Map<String, TlabAccumulator> buckets = new HashMap<>();

        for (IItemIterable itemIter : insideTlab) {
            IMemberAccessor<String, IItem> threadNameAccessor =
                    getAccessor(itemIter, JdkAttributes.EVENT_THREAD_NAME);
            IMemberAccessor<IQuantity, IItem> allocSizeAccessor =
                    getAccessor(itemIter, JdkAttributes.ALLOCATION_SIZE);
            for (IItem item : itemIter) {
                String threadName = threadNameAccessor != null ? threadNameAccessor.getMember(item) : null;
                if (threadName == null) {
                    continue;
                }
                long size = 0;
                if (allocSizeAccessor != null) {
                    IQuantity qty = allocSizeAccessor.getMember(item);
                    if (qty != null) {
                        size = qty.longValue();
                    }
                }
                buckets.computeIfAbsent(threadName, k -> new TlabAccumulator(threadName))
                        .addInside(size);
            }
        }

        for (IItemIterable itemIter : outsideTlab) {
            IMemberAccessor<String, IItem> threadNameAccessor =
                    getAccessor(itemIter, JdkAttributes.EVENT_THREAD_NAME);
            IMemberAccessor<IQuantity, IItem> allocSizeAccessor =
                    getAccessor(itemIter, JdkAttributes.ALLOCATION_SIZE);
            for (IItem item : itemIter) {
                String threadName = threadNameAccessor != null ? threadNameAccessor.getMember(item) : null;
                if (threadName == null) {
                    continue;
                }
                long size = 0;
                if (allocSizeAccessor != null) {
                    IQuantity qty = allocSizeAccessor.getMember(item);
                    if (qty != null) {
                        size = qty.longValue();
                    }
                }
                buckets.computeIfAbsent(threadName, k -> new TlabAccumulator(threadName))
                        .addOutside(size);
            }
        }

        List<TlabAllocation> results = new ArrayList<>();
        for (TlabAccumulator acc : buckets.values()) {
            double insideAvg = acc.insideCount > 0 ? (double) acc.insideTotalSize / acc.insideCount : 0;
            double outsideAvg = acc.outsideCount > 0 ? (double) acc.outsideTotalSize / acc.outsideCount : 0;
            results.add(new TlabAllocation(acc.threadName, acc.insideCount, acc.outsideCount,
                    insideAvg, outsideAvg, acc.insideTotalSize, acc.outsideTotalSize));
        }
        results.sort(Comparator.comparingLong(TlabAllocation::insideTotalSize).reversed());
        return JmcResultLimiter.limitRows(results);
    }

    @Override
    public ChartDefinition loadTlabAllocationTimeline(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection insideTlab = events.apply(JdkFilters.ALLOC_INSIDE_TLAB);
        IItemCollection outsideTlab = events.apply(JdkFilters.ALLOC_OUTSIDE_TLAB);

        Map<Long, Long> bytesBySecond = new LinkedHashMap<>();
        collectTimelineBytes(insideTlab, bytesBySecond);
        collectTimelineBytes(outsideTlab, bytesBySecond);

        if (bytesBySecond.isEmpty()) {
            return new ChartDefinition("Time", "Bytes", List.of());
        }

        List<ChartDataPoint> points = new ArrayList<>(bytesBySecond.entrySet().stream()
                .map(entry -> new ChartDataPoint(entry.getKey(), entry.getValue()))
                .toList());
        points.sort(Comparator.comparingDouble(ChartDataPoint::x));
        ChartSeries series = new ChartSeries("allocations", "Allocations",
                timelineSeriesType(), List.copyOf(points));
        return JmcResultLimiter.limitChart(new ChartDefinition("Time", "Bytes", List.of(series)));
    }

    static ChartSeriesType timelineSeriesType() {
        return ChartSeriesType.LINE;
    }

    private void collectTimelineBytes(IItemCollection collection, Map<Long, Long> bytesBySecond) {
        for (IItemIterable itemIter : collection) {
            @SuppressWarnings("unchecked")
            IMemberAccessor<IQuantity, IItem> startTimeAccessor =
                    (IMemberAccessor<IQuantity, IItem>) JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> allocSizeAccessor =
                    getAccessor(itemIter, JdkAttributes.ALLOCATION_SIZE);
            for (IItem item : itemIter) {
                IQuantity startTime = startTimeAccessor != null ? startTimeAccessor.getMember(item) : null;
                if (startTime == null) {
                    continue;
                }
                long timeMs = UnitLookup.toDate(startTime).toInstant().toEpochMilli();
                long bucket = timeMs / TIMELINE_BUCKET_MILLIS * TIMELINE_BUCKET_MILLIS;
                long size = 0;
                if (allocSizeAccessor != null) {
                    IQuantity qty = allocSizeAccessor.getMember(item);
                    if (qty != null) {
                        size = qty.longValue();
                    }
                }
                bytesBySecond.merge(bucket, size, Long::sum);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> IMemberAccessor<T, IItem> getAccessor(
            IItemIterable itemIterable, IAttribute<T> attribute) {
        return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
    }

    private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

    private static final class TlabAccumulator {
        final String threadName;
        long insideCount;
        long outsideCount;
        long insideTotalSize;
        long outsideTotalSize;

        TlabAccumulator(String threadName) {
            this.threadName = threadName;
        }

        void addInside(long size) {
            insideCount++;
            insideTotalSize += size;
        }

        void addOutside(long size) {
            outsideCount++;
            outsideTotalSize += size;
        }
    }
}
