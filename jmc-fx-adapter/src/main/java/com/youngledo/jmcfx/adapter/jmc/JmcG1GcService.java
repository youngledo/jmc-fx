package com.youngledo.jmcfx.adapter.jmc;

import static org.openjdk.jmc.common.item.Attribute.attr;

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
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

import com.youngledo.jmcfx.domain.model.G1GcRegionState;
import com.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import com.youngledo.jmcfx.domain.model.G1GcReport;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.G1GcService;

public class JmcG1GcService implements G1GcService {

    private static final IAttribute<String> REGION_TYPE = attr("type", "Type", UnitLookup.PLAIN_TEXT);
    private static final IAttribute<String> REGION_TO_TYPE = attr("to", "To Type", UnitLookup.PLAIN_TEXT);
    private static final IAttribute<IQuantity> REGION_CAPACITY = attr("capacity", "Capacity", UnitLookup.MEMORY);
    private static final IAttribute<IQuantity> REGION_USED = attr("used", "Used", UnitLookup.MEMORY);
    private static final IAttribute<IQuantity> REGION_INDEX = attr("index", "Index", UnitLookup.NUMBER);
    private static final IAttribute<IQuantity> REGION_ALLOC_CONTEXT =
            attr("allocContext", "Allocation Context", UnitLookup.NUMBER);

    @Override
    public G1GcReport loadG1GcReport(RecordingSummary recording) {
        IItemCollection events = JmcRecordingDataCache.SHARED.events(recording);
        List<G1GcRegionState> snapshots = readRegionStates(events.apply(
                ItemFilters.type(JdkTypeIDs.GC_G1_HEAP_REGION_INFORMATION)), "Snapshot", false);
        List<G1GcRegionState> transitions = readRegionStates(events.apply(
                ItemFilters.type(JdkTypeIDs.GC_G1_HEAP_REGION_TYPE_CHANGE)), "Transition", true);
        List<G1GcRegionState> regionStates = new ArrayList<>(snapshots.size() + transitions.size());
        regionStates.addAll(snapshots);
        regionStates.addAll(transitions);
        regionStates.sort(regionStateComparator());

        List<G1GcRegionSummary> summaries = summarizeLatestRegions(regionStates);
        List<GcEvent> pauses = readGcPauses(events);
        long usedBytes = summaries.stream().mapToLong(G1GcRegionSummary::usedBytes).sum();
        long capacityBytes = summaries.stream().mapToLong(G1GcRegionSummary::capacityBytes).sum();
        long regionCount = summaries.stream().mapToLong(G1GcRegionSummary::regionCount).sum();
        Instant lastSnapshotTime = snapshots.stream()
                .map(G1GcRegionState::startTime)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);

        return new G1GcReport(
                snapshots.size(),
                transitions.size(),
                pauses.size(),
                regionCount,
                usedBytes,
                capacityBytes,
                lastSnapshotTime,
                summaries,
                JmcResultLimiter.limitRows(regionStates),
                pauses);
    }

    private List<G1GcRegionState> readRegionStates(IItemCollection events, String eventKind, boolean transition) {
        List<G1GcRegionState> result = new ArrayList<>();
        events.stream().flatMap(IItemIterable::stream).forEach(item -> {
            String previousType = transition ? readString(REGION_TYPE, item) : "";
            String type = transition ? readString(REGION_TO_TYPE, item) : readString(REGION_TYPE, item);
            result.add(new G1GcRegionState(
                    readLong(REGION_INDEX, item),
                    type,
                    previousType,
                    eventKind,
                    readLongBytes(REGION_USED, item),
                    readLongBytes(REGION_CAPACITY, item),
                    readLong(REGION_ALLOC_CONTEXT, item),
                    readInstant(JfrAttributes.START_TIME, item)));
        });
        return result;
    }

    private List<G1GcRegionSummary> summarizeLatestRegions(List<G1GcRegionState> regionStates) {
        Map<Long, G1GcRegionState> latestByRegion = new LinkedHashMap<>();
        for (G1GcRegionState state : regionStates) {
            latestByRegion.merge(state.regionIndex(), state, (left, right) ->
                    regionStateComparator().compare(left, right) <= 0 ? right : left);
        }

        Map<String, RegionAccumulator> byType = new LinkedHashMap<>();
        latestByRegion.values().stream()
                .sorted(Comparator.comparing(G1GcRegionState::type))
                .forEach(state -> byType.computeIfAbsent(blankToUnknown(state.type()), RegionAccumulator::new)
                        .add(state));
        return List.copyOf(byType.values().stream()
                .map(RegionAccumulator::toSummary)
                .toList());
    }

    private List<GcEvent> readGcPauses(IItemCollection events) {
        List<GcEvent> result = new ArrayList<>();
        events.apply(JdkFilters.GARBAGE_COLLECTION).stream()
                .flatMap(IItemIterable::stream)
                .forEach(item -> result.add(new GcEvent(
                        readLong(JdkAttributes.GC_ID, item),
                        readString(JdkAttributes.GC_NAME, item),
                        readString(JdkAttributes.GC_CAUSE, item),
                        readLongMicros(JdkAttributes.GC_LONGEST_PAUSE, item),
                        readDurationMicros(item),
                        readInstant(JfrAttributes.START_TIME, item))));
        result.sort(Comparator.comparingLong(GcEvent::gcId));
        return JmcResultLimiter.limitRows(result);
    }

    private static Comparator<G1GcRegionState> regionStateComparator() {
        return Comparator.comparing(G1GcRegionState::startTime)
                .thenComparingLong(G1GcRegionState::regionIndex)
                .thenComparing(G1GcRegionState::eventKind);
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

    @SuppressWarnings("unchecked")
    private Object readRaw(IAttribute<?> attribute, IItem item) {
        IMemberAccessor<Object, IItem> accessor =
                (IMemberAccessor<Object, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    private static String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private static final class RegionAccumulator {
        private final String type;
        private long count;
        private long usedBytes;
        private long capacityBytes;

        private RegionAccumulator(String type) {
            this.type = type;
        }

        private void add(G1GcRegionState state) {
            count++;
            usedBytes += state.usedBytes();
            capacityBytes += state.capacityBytes();
        }

        private G1GcRegionSummary toSummary() {
            return new G1GcRegionSummary(type, count, usedBytes, capacityBytes);
        }
    }
}
