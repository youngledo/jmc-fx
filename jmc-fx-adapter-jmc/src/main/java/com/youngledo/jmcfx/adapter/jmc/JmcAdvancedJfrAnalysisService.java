package com.youngledo.jmcfx.adapter.jmc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.MemoryIssue;
import com.youngledo.jmcfx.domain.model.MemoryIssueCategory;
import com.youngledo.jmcfx.domain.model.MemoryIssueSeverity;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;

public class JmcAdvancedJfrAnalysisService implements AdvancedJfrAnalysisService {

    private static final int MAX_BUCKETS = 80;
    private static final int MAX_EVENT_TYPES = 40;
    private static final int MAX_MEMORY_ISSUES = 40;
    private static final long WARNING_BYTES = 32L * 1024 * 1024;
    private static final long CRITICAL_BYTES = 256L * 1024 * 1024;

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

    @Override
    public MemoryAnalysisReport loadMemoryAnalysis(RecordingSummary recording, int maxIssues) {
        int issueLimit = normalizedMaxMemoryIssues(maxIssues);
        JmcRecordingDataCache.RecordingData data = JmcRecordingDataCache.SHARED.recording(recording);
        Map<MemoryIssueKey, MemoryIssueAccumulator> issues = new HashMap<>();

        aggregateAllocationHotspots(data.events(), issues);
        aggregateOutsideTlabPressure(data.events(), issues);
        aggregateRetainedObjects(data.events(), issues);

        List<MemoryIssue> allIssues = issues.values().stream()
                .map(MemoryIssueAccumulator::toIssue)
                .sorted(Comparator.comparingInt((MemoryIssue issue) -> severityRank(issue.severity())).reversed()
                        .thenComparing(Comparator.comparingLong(MemoryIssue::estimatedBytes).reversed())
                        .thenComparing(Comparator.comparingLong(MemoryIssue::count).reversed())
                        .thenComparing(MemoryIssue::subject))
                .toList();
        long totalEstimatedBytes = allIssues.stream().mapToLong(MemoryIssue::estimatedBytes).sum();
        long totalCount = allIssues.stream().mapToLong(MemoryIssue::count).sum();
        return new MemoryAnalysisReport(totalEstimatedBytes, totalCount,
                allIssues.stream().limit(issueLimit).toList());
    }

    int normalizedBucketCount(int bucketCount) {
        return Math.max(1, Math.min(MAX_BUCKETS, bucketCount));
    }

    int normalizedMaxEventTypes(int maxEventTypes) {
        return Math.max(1, Math.min(MAX_EVENT_TYPES, maxEventTypes));
    }

    int normalizedMaxMemoryIssues(int maxIssues) {
        return Math.max(1, Math.min(MAX_MEMORY_ISSUES, maxIssues));
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

    private void aggregateAllocationHotspots(IItemCollection events,
            Map<MemoryIssueKey, MemoryIssueAccumulator> issues) {
        aggregateByType(events, JdkFilters.ALLOC_ALL, JdkAttributes.ALLOCATION_CLASS,
                JdkAttributes.ALLOCATION_SIZE, MemoryIssueCategory.ALLOCATION_HOTSPOT,
                "High allocation volume", "Review allocation rate for this type.", issues);
    }

    private void aggregateOutsideTlabPressure(IItemCollection events,
            Map<MemoryIssueKey, MemoryIssueAccumulator> issues) {
        aggregateByType(events, JdkFilters.ALLOC_OUTSIDE_TLAB, JdkAttributes.EVENT_THREAD_NAME,
                JdkAttributes.ALLOCATION_SIZE, MemoryIssueCategory.OUTSIDE_TLAB,
                "Outside-TLAB allocations", "Review large allocations on this thread.", issues);
    }

    private void aggregateRetainedObjects(IItemCollection events,
            Map<MemoryIssueKey, MemoryIssueAccumulator> issues) {
        aggregateByType(events, ItemFilters.type(JdkTypeIDs.OLD_OBJECT_SAMPLE), JdkAttributes.OLD_OBJECT_CLASS,
                JdkAttributes.SAMPLE_WEIGHT, MemoryIssueCategory.RETAINED_OBJECT,
                "Retained old-object samples", "Inspect retaining paths for this object type.", issues);
    }

    private <S> void aggregateByType(IItemCollection events, IItemFilter filter,
            org.openjdk.jmc.common.item.IAttribute<S> subjectAttribute,
            org.openjdk.jmc.common.item.IAttribute<IQuantity> byteAttribute,
            MemoryIssueCategory category, String evidencePrefix, String recommendation,
            Map<MemoryIssueKey, MemoryIssueAccumulator> issues) {
        IItemCollection filtered = events.apply(filter);
        for (IItemIterable iterable : filtered) {
            IMemberAccessor<S, IItem> subjectAccessor = subjectAttribute.getAccessor(iterable.getType());
            IMemberAccessor<IQuantity, IItem> byteAccessor = byteAttribute.getAccessor(iterable.getType());
            if (subjectAccessor == null) {
                continue;
            }
            for (IItem item : iterable) {
                String subject = subject(subjectAccessor.getMember(item));
                long bytes = bytes(byteAccessor == null ? null : byteAccessor.getMember(item));
                MemoryIssueKey key = new MemoryIssueKey(category, subject);
                issues.computeIfAbsent(key, ignored -> new MemoryIssueAccumulator(category, subject,
                        evidencePrefix, recommendation)).add(bytes);
            }
        }
    }

    private String subject(Object value) {
        return switch (value) {
            case null -> "Unknown";
            case IMCType type -> type.getFullName() == null || type.getFullName().isBlank()
                    ? type.getTypeName()
                    : type.getFullName();
            default -> {
                String text = value.toString();
                yield text == null || text.isBlank() ? "Unknown" : text;
            }
        };
    }

    private long bytes(IQuantity quantity) {
        return quantity == null ? 0 : Math.max(0, quantity.clampedLongValueIn(UnitLookup.BYTE));
    }

    private MemoryIssueSeverity severity(long bytes) {
        if (bytes >= CRITICAL_BYTES) {
            return MemoryIssueSeverity.CRITICAL;
        }
        if (bytes >= WARNING_BYTES) {
            return MemoryIssueSeverity.WARNING;
        }
        return MemoryIssueSeverity.INFO;
    }

    private static int severityRank(MemoryIssueSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case INFO -> 1;
        };
    }

    private double score(long bytes) {
        if (bytes >= CRITICAL_BYTES) {
            return 100;
        }
        if (bytes >= WARNING_BYTES) {
            return 60 + Math.min(39, (bytes - WARNING_BYTES) * 40.0 / (CRITICAL_BYTES - WARNING_BYTES));
        }
        return Math.min(59, bytes * 60.0 / WARNING_BYTES);
    }

    private record EventSample(String eventTypeId, String label, List<String> categoryPath, Instant startTime) {
    }

    private record MemoryIssueKey(MemoryIssueCategory category, String subject) {
    }

    private final class MemoryIssueAccumulator {
        private final MemoryIssueCategory category;
        private final String subject;
        private final String evidencePrefix;
        private final String recommendation;
        private long estimatedBytes;
        private long count;

        MemoryIssueAccumulator(MemoryIssueCategory category, String subject, String evidencePrefix,
                String recommendation) {
            this.category = category;
            this.subject = subject;
            this.evidencePrefix = evidencePrefix;
            this.recommendation = recommendation;
        }

        void add(long bytes) {
            estimatedBytes += bytes;
            count++;
        }

        MemoryIssue toIssue() {
            return new MemoryIssue(category, severity(estimatedBytes), subject, estimatedBytes, count,
                    score(estimatedBytes), evidencePrefix + ": " + count + " events, "
                            + estimatedBytes + " estimated bytes.",
                    recommendation);
        }
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
