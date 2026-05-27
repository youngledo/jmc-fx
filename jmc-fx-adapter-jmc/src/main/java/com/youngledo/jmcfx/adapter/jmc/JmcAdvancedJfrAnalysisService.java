package com.youngledo.jmcfx.adapter.jmc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;

public class JmcAdvancedJfrAnalysisService implements AdvancedJfrAnalysisService {

    private static final int MAX_BUCKETS = 80;
    private static final int MAX_EVENT_TYPES = 40;

    @Override
    public EventHeatmap loadEventHeatmap(RecordingSummary recording, int bucketCount, int maxEventTypes) {
        int buckets = normalizedBucketCount(bucketCount);
        int rowLimit = normalizedMaxEventTypes(maxEventTypes);
        JmcRecordingDataCache.RecordingData data = JmcRecordingDataCache.SHARED.recording(recording);
        List<EventSample> samples = samples(data.events(), categoryPathByType(data));
        if (samples.isEmpty()) {
            return new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusMillis(1), buckets, List.of());
        }
        Instant start = samples.stream()
                .map(EventSample::startTime)
                .min(Instant::compareTo)
                .orElse(Instant.EPOCH);
        Instant end = samples.stream()
                .map(EventSample::startTime)
                .max(Instant::compareTo)
                .orElse(start);
        if (!end.isAfter(start)) {
            end = start.plusMillis(1);
        }
        long rangeMillis = Math.max(1, end.toEpochMilli() - start.toEpochMilli());
        Map<String, EventTypeAccumulator> byType = new HashMap<>();
        for (EventSample sample : samples) {
            int bucket = bucketIndex(sample.startTime(), start, rangeMillis, buckets);
            byType.computeIfAbsent(sample.eventTypeId(), ignored ->
                    new EventTypeAccumulator(sample.eventTypeId(), sample.label(), sample.categoryPath(), buckets))
                    .add(bucket);
        }
        List<EventHeatmapRow> rows = byType.values().stream()
                .sorted(Comparator.comparingLong(EventTypeAccumulator::totalCount).reversed()
                        .thenComparing(EventTypeAccumulator::label))
                .limit(rowLimit)
                .map(accumulator -> accumulator.toRow(start, rangeMillis))
                .toList();
        return new EventHeatmap(start, end, buckets, rows);
    }

    int normalizedBucketCount(int bucketCount) {
        return Math.max(1, Math.min(MAX_BUCKETS, bucketCount));
    }

    int normalizedMaxEventTypes(int maxEventTypes) {
        return Math.max(1, Math.min(MAX_EVENT_TYPES, maxEventTypes));
    }

    private int bucketIndex(Instant eventTime, Instant start, long rangeMillis, int bucketCount) {
        long offsetMillis = Math.max(0, eventTime.toEpochMilli() - start.toEpochMilli());
        long bucket = offsetMillis * bucketCount / rangeMillis;
        return (int) Math.min(bucketCount - 1, Math.max(0, bucket));
    }

    private Map<String, List<String>> categoryPathByType(JmcRecordingDataCache.RecordingData data) {
        Map<String, List<String>> categories = new HashMap<>();
        Arrays.stream(data.eventArrays().getArrays())
                .forEach(eventArray -> categories.put(eventArray.getType().getIdentifier(),
                        categoryPath(eventArray.getTypeCategory())));
        return categories;
    }

    private List<EventSample> samples(IItemCollection events, Map<String, List<String>> categoryPathByType) {
        List<EventSample> samples = new ArrayList<>();
        for (IItemIterable iterable : events) {
            String eventTypeId = iterable.getType().getIdentifier();
            String label = label(iterable);
            List<String> categoryPath = categoryPathByType.getOrDefault(eventTypeId, List.of("Uncategorized"));
            var startAccessor = JfrAttributes.START_TIME.getAccessor(iterable.getType());
            for (IItem item : iterable) {
                IQuantity startTime = startAccessor.getMember(item);
                if (startTime != null) {
                    samples.add(new EventSample(eventTypeId, label, categoryPath,
                            Instant.ofEpochMilli(startTime.clampedLongValueIn(UnitLookup.EPOCH_MS))));
                }
            }
        }
        return samples;
    }

    private String label(IItemIterable iterable) {
        String name = iterable.getType().getName();
        return name == null || name.isBlank() ? iterable.getType().getIdentifier() : name;
    }

    private List<String> categoryPath(String[] category) {
        if (category == null || category.length == 0) {
            return List.of("Uncategorized");
        }
        List<String> path = Arrays.stream(category)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        return path.isEmpty() ? List.of("Uncategorized") : path;
    }

    private record EventSample(String eventTypeId, String label, List<String> categoryPath, Instant startTime) {
    }

    private static final class EventTypeAccumulator {
        private final String eventTypeId;
        private final String label;
        private final List<String> categoryPath;
        private final long[] counts;
        private long totalCount;

        EventTypeAccumulator(String eventTypeId, String label, List<String> categoryPath, int bucketCount) {
            this.eventTypeId = eventTypeId;
            this.label = label == null || label.isBlank() ? eventTypeId : label;
            this.categoryPath = List.copyOf(categoryPath);
            this.counts = new long[bucketCount];
        }

        void add(int bucket) {
            counts[bucket]++;
            totalCount++;
        }

        long totalCount() {
            return totalCount;
        }

        String label() {
            return label;
        }

        EventHeatmapRow toRow(Instant start, long rangeMillis) {
            List<EventHeatmapCell> cells = new ArrayList<>(counts.length);
            for (int i = 0; i < counts.length; i++) {
                long bucketStartOffset = Math.floorDiv(rangeMillis * i, counts.length);
                long bucketEndOffset = Math.floorDiv(rangeMillis * (i + 1), counts.length);
                cells.add(new EventHeatmapCell(eventTypeId, start.plusMillis(bucketStartOffset),
                        start.plusMillis(Math.max(bucketStartOffset + 1, bucketEndOffset)), counts[i]));
            }
            return new EventHeatmapRow(eventTypeId, label, categoryPath, totalCount, cells);
        }
    }
}
