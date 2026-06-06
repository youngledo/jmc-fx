package io.github.youngledo.jmcfx.adapter.jmc;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectSummary;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.stats.ObjectHistogram;
import org.openjdk.jmc.joverflow.stats.StandardStatsCalculator;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

public final class JmcHeapDumpBrowsingService implements HeapDumpBrowsingService {

    static final String BROWSING_UNAVAILABLE = "Detailed heap browsing is not available for this heap dump.";

    @Override
    public HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.groupKind() != HeapDumpObjectGroupKind.CLASS) {
            return unavailableWindow(request);
        }
        HeapStats stats = loadStats(request);
        ObjectHistogram histogram = stats.objHisto;
        if (histogram == null) {
            return unavailableWindow(request);
        }
        List<HeapDumpObjectGroup> rows = sortedEntries(histogram, request)
                .filter(entry -> matchesSearch(entry.getClazz(), request.searchText()))
                .map(this::toGroup)
                .toList();
        return slice(rows, request);
    }

    @Override
    public HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(groupId, "groupId");
        if (request.groupKind() != HeapDumpObjectGroupKind.CLASS) {
            return unavailableDetail(request, groupId);
        }
        HeapStats stats = loadStats(request);
        ObjectHistogram histogram = stats.objHisto;
        if (histogram == null) {
            return unavailableDetail(request, groupId);
        }
        HeapDumpObjectGroup group = sortedEntries(histogram, request)
                .map(this::toGroup)
                .filter(candidate -> candidate.id().equals(groupId))
                .findFirst()
                .orElseGet(() -> new HeapDumpObjectGroup(groupId, groupId, HeapDumpObjectGroupKind.CLASS,
                        0, 0, 0, 0, false));
        List<HeapDumpObjectSummary> objects = statsSnapshot(stats).getObjects().stream()
                .filter(object -> object.getClazz().getName().equals(groupId))
                .skip(request.offset())
                .limit(request.limit())
                .map(this::toObjectSummary)
                .toList();
        long totalCount = group.objectCount();
        boolean truncated = request.offset() + objects.size() < totalCount;
        String note = objects.isEmpty() ? BROWSING_UNAVAILABLE : "";
        return new HeapDumpObjectGroupDetail(group,
                new HeapDumpBrowseWindow<>(objects, request.offset(), request.limit(), totalCount, truncated), note);
    }

    @Override
    public HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request) {
        Objects.requireNonNull(request, "request");
        return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, true);
    }

    private HeapStats loadStats(HeapDumpBrowseRequest request) {
        try {
            ReadBuffer.Factory readBufferFactory = new ReadBuffer.CachedReadBufferFactory(request.path().toString(), 0);
            HeapDumpReader reader = HeapDumpReader.createReader(readBufferFactory, 0, new VerboseOutputCollector());
            Snapshot snapshot = reader.read();
            return new StandardStatsCalculator(snapshot, new BatchProblemRecorder(), false).calculate();
        } catch (DumpCorruptedException | HprofParsingCancelledException exception) {
            throw new JmcFxException("Unable to browse heap dump: " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw new JmcFxException("Unable to browse heap dump: " + exception.getMessage(), exception);
        }
    }

    private Stream<ObjectHistogram.Entry> sortedEntries(ObjectHistogram histogram, HeapDumpBrowseRequest request) {
        List<ObjectHistogram.Entry> entries = histogram.getListSortedByInclusiveSize(0);
        java.util.Comparator<ObjectHistogram.Entry> comparator = switch (request.sort()) {
            case RETAINED_SIZE_BYTES -> java.util.Comparator.comparingLong(ObjectHistogram.Entry::getTotalInclusiveSize);
            case SHALLOW_SIZE_BYTES -> java.util.Comparator.comparingLong(ObjectHistogram.Entry::getTotalShallowSize);
            case OBJECT_COUNT -> java.util.Comparator.comparingInt(ObjectHistogram.Entry::getNumInstances);
            case LABEL -> java.util.Comparator.comparing(entry -> entry.getClazz().getName(), String.CASE_INSENSITIVE_ORDER);
        };
        if (!request.ascending()) {
            comparator = comparator.reversed();
        }
        return entries.stream().sorted(comparator);
    }

    private HeapDumpObjectGroup toGroup(ObjectHistogram.Entry entry) {
        JavaClass clazz = entry.getClazz();
        return new HeapDumpObjectGroup(clazz.getName(), clazz.getHumanFriendlyNameWithLoaderIfNeeded(),
                HeapDumpObjectGroupKind.CLASS, entry.getNumInstances(), entry.getTotalShallowSize(),
                entry.getTotalInclusiveSize(), 0, false);
    }

    private HeapDumpObjectSummary toObjectSummary(JavaLazyReadObject object) {
        return new HeapDumpObjectSummary(object.idAsString(), object.getClazz().getHumanFriendlyNameWithLoaderIfNeeded(),
                object.getSize(), object.getImplInclusiveSize(), 0, 0, true);
    }

    private boolean matchesSearch(JavaClass clazz, String searchText) {
        return searchText == null || searchText.isBlank()
                || clazz.getName().toLowerCase(java.util.Locale.ROOT)
                        .contains(searchText.toLowerCase(java.util.Locale.ROOT));
    }

    private HeapDumpBrowseWindow<HeapDumpObjectGroup> unavailableWindow(HeapDumpBrowseRequest request) {
        return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, true);
    }

    private HeapDumpObjectGroupDetail unavailableDetail(HeapDumpBrowseRequest request, String groupId) {
        HeapDumpObjectGroup group = new HeapDumpObjectGroup(groupId, groupId, request.groupKind(),
                0, 0, 0, 0, false);
        return new HeapDumpObjectGroupDetail(group, new HeapDumpBrowseWindow<>(List.of(), 0, request.limit(), 0, true),
                BROWSING_UNAVAILABLE);
    }

    private HeapDumpBrowseWindow<HeapDumpObjectGroup> slice(List<HeapDumpObjectGroup> rows, HeapDumpBrowseRequest request) {
        List<HeapDumpObjectGroup> windowRows = rows.stream()
                .skip(request.offset())
                .limit(request.limit())
                .toList();
        boolean truncated = request.offset() + windowRows.size() < rows.size();
        return new HeapDumpBrowseWindow<>(windowRows, request.offset(), request.limit(), rows.size(), truncated);
    }

    private Snapshot statsSnapshot(HeapStats stats) {
        return stats.objHisto.getListSortedByInclusiveSize(0).stream()
                .findFirst()
                .map(entry -> entry.getClazz().getSnapshot())
                .orElseThrow(() -> new JmcFxException("Unable to browse heap dump: object histogram is empty"));
    }
}
