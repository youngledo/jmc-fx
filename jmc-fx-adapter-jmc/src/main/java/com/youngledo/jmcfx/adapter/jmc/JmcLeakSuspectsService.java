package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.LeakSuspectsService;

public class JmcLeakSuspectsService implements LeakSuspectsService {

    @Override
    public List<LeakCandidate> loadLeakCandidates(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection oldObjects = events.apply(ItemFilters.type(JdkTypeIDs.OLD_OBJECT_SAMPLE));
        if (!oldObjects.hasItems()) {
            return List.of();
        }

        Map<String, LeakAccumulator> buckets = new HashMap<>();
        for (IItemIterable itemIter : oldObjects) {
            IMemberAccessor<IMCType, IItem> classAccessor =
                    getAccessor(itemIter, JdkAttributes.OLD_OBJECT_CLASS);
            IMemberAccessor<String, IItem> descAccessor =
                    getAccessor(itemIter, JdkAttributes.OLD_OBJECT_DESCRIPTION);
            IMemberAccessor<IQuantity, IItem> addressAccessor =
                    getAccessor(itemIter, JdkAttributes.OLD_OBJECT_ADDRESS);
            IMemberAccessor<IQuantity, IItem> weightAccessor =
                    getAccessor(itemIter, JdkAttributes.SAMPLE_WEIGHT);
            for (IItem item : itemIter) {
                IMCType type = classAccessor != null ? classAccessor.getMember(item) : null;
                String className = type != null ? type.getFullName() : "<unknown>";
                String description = descAccessor != null ? descAccessor.getMember(item) : "";
                String address = "";
                if (addressAccessor != null) {
                    IQuantity addr = addressAccessor.getMember(item);
                    if (addr != null) {
                        address = "0x" + Long.toHexString(addr.longValue());
                    }
                }
                double weight = 1.0;
                if (weightAccessor != null) {
                    IQuantity w = weightAccessor.getMember(item);
                    if (w != null) {
                        weight = w.doubleValue();
                    }
                }
                buckets.computeIfAbsent(className,
                        k -> new LeakAccumulator(className)).add(description, address, weight);
            }
        }

        double totalWeight = buckets.values().stream().mapToDouble(a -> a.totalWeight).sum();
        List<LeakCandidate> results = new ArrayList<>();
        for (LeakAccumulator acc : buckets.values()) {
            double relevance = totalWeight > 0 ? (acc.totalWeight * 100.0) / totalWeight : 0;
            results.add(new LeakCandidate(acc.className, acc.count, acc.description,
                    acc.address, relevance));
        }
        results.sort(Comparator.comparingDouble(LeakCandidate::relevance).reversed());
        return List.copyOf(results);
    }

    @Override
    public LeakReferenceNode loadLeakReferenceTree(RecordingSummary recording, int candidateIndex) {
        IItemCollection events = loadEvents(recording);
        IItemCollection oldObjects = events.apply(ItemFilters.type(JdkTypeIDs.OLD_OBJECT_SAMPLE));
        if (!oldObjects.hasItems()) {
            return LeakReferenceNode.EMPTY;
        }

        List<IItem> items = new ArrayList<>();
        for (IItemIterable itemIter : oldObjects) {
            for (IItem item : itemIter) {
                items.add(item);
            }
        }
        if (candidateIndex >= items.size()) {
            return LeakReferenceNode.EMPTY;
        }

        IItem targetItem = items.get(candidateIndex);
        for (IItemIterable itemIter : oldObjects) {
            IMemberAccessor<IMCType, IItem> classAccessor =
                    getAccessor(itemIter, JdkAttributes.OLD_OBJECT_CLASS);
            IMemberAccessor<String, IItem> descAccessor =
                    getAccessor(itemIter, JdkAttributes.OLD_OBJECT_DESCRIPTION);
            IMemberAccessor<IQuantity, IItem> addressAccessor =
                    getAccessor(itemIter, JdkAttributes.OLD_OBJECT_ADDRESS);
            int idx = 0;
            for (IItem item : itemIter) {
                if (idx == candidateIndex) {
                    String className = "<unknown>";
                    if (classAccessor != null) {
                        IMCType type = classAccessor.getMember(item);
                        if (type != null) {
                            className = type.getFullName();
                        }
                    }
                    String description = descAccessor != null ? descAccessor.getMember(item) : "";
                    String address = "";
                    if (addressAccessor != null) {
                        IQuantity addr = addressAccessor.getMember(item);
                        if (addr != null) {
                            address = "0x" + Long.toHexString(addr.longValue());
                        }
                    }
                    String label = className + " @ " + address;
                    return new LeakReferenceNode(label, description != null ? description : "",
                            address, List.of());
                }
                idx++;
            }
        }
        return LeakReferenceNode.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private static <T> IMemberAccessor<T, IItem> getAccessor(
            IItemIterable itemIterable, IAttribute<T> attribute) {
        return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
    }

    private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

    private static final class LeakAccumulator {
        final String className;
        int count;
        String description;
        String address;
        double totalWeight;

        LeakAccumulator(String className) {
            this.className = className;
        }

        void add(String description, String address, double weight) {
            count++;
            this.description = description;
            this.address = address;
            this.totalWeight += weight;
        }
    }
}
