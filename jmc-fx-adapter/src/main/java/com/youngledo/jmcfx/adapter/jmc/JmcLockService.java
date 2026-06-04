package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;

import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.LockService;

/// JMC-backed lock instance analysis adapter.
///
/// Uses {@link JdkFilters#MONITOR_ENTER} to extract lock contention events
/// and {@link JdkFilters#MONITOR_INFLATE} for inflation statistics.
public class JmcLockService implements LockService {

	@Override
	public List<LockHistogram> loadLockHistogram(RecordingSummary recording, LockGrouping grouping) {
		IItemCollection events = loadEvents(recording);
		IItemCollection lockEvents = events.apply(JdkFilters.MONITOR_ENTER);
		if (!lockEvents.hasItems()) {
			return List.of();
		}

		// Pre-collect inflate counts by address
		Map<String, Long> inflateByAddress = collectInflateCounts(events);

		Map<String, LockAccumulator> buckets = new HashMap<>();
		for (IItemIterable itemIterable : lockEvents) {
			IMemberAccessor<IMCType, IItem> classAccessor =
					getAccessor(itemIterable, JdkAttributes.MONITOR_CLASS);
			IMemberAccessor<IQuantity, IItem> addressAccessor =
					getAccessor(itemIterable, JdkAttributes.MONITOR_ADDRESS);
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					JfrAttributes.DURATION.getAccessor(itemIterable.getType());
			IMemberAccessor<IMCThread, IItem> threadAccessor =
					JfrAttributes.EVENT_THREAD.getAccessor(itemIterable.getType());
			for (IItem item : itemIterable) {
				String className = null;
				if (classAccessor != null) {
					IMCType type = classAccessor.getMember(item);
					if (type != null) {
						className = type.getFullName();
					}
				}
				String address = null;
				if (addressAccessor != null) {
					IQuantity addrQty = addressAccessor.getMember(item);
					if (addrQty != null) {
						address = addrQty.persistableString();
					}
				}
				String threadName = null;
				if (threadAccessor != null) {
					IMCThread thread = threadAccessor.getMember(item);
					if (thread != null) {
						threadName = thread.getThreadName();
					}
				}
				long durMs = 0;
				if (durationAccessor != null) {
					IQuantity dur = durationAccessor.getMember(item);
					if (dur != null) {
						try {
							durMs = dur.longValueIn(UnitLookup.MILLISECOND);
						} catch (QuantityConversionException e) {
							durMs = 0;
						}
					}
				}

				String key = switch (grouping) {
					case BY_CLASS -> className != null ? className : "<unknown>";
					case BY_ADDRESS -> address != null ? address : "<unknown>";
					case BY_THREAD -> threadName != null ? threadName : "<unknown>";
				};

				LockAccumulator acc = buckets.computeIfAbsent(key, LockAccumulator::new);
				acc.count++;
				acc.totalDuration += durMs;
				acc.maxDuration = Math.max(acc.maxDuration, durMs);
				if (threadName != null) {
					acc.threads.add(threadName);
				}
				if (address != null) {
					acc.addresses.add(address);
				}
			}
		}

		List<LockHistogram> results = new ArrayList<>();
		for (LockAccumulator acc : buckets.values()) {
			double avg = acc.count > 0 ? (double) acc.totalDuration / acc.count : 0;
			long inflateCount = 0;
			// For BY_ADDRESS grouping, count inflates directly
			if (grouping == LockGrouping.BY_ADDRESS) {
				inflateCount = inflateByAddress.getOrDefault(acc.key, 0L);
			} else {
				// For BY_CLASS or BY_THREAD, sum inflates across all addresses in the group
				for (String addr : acc.addresses) {
					inflateCount += inflateByAddress.getOrDefault(addr, 0L);
				}
			}
			results.add(new LockHistogram(acc.key, acc.count, acc.totalDuration,
					acc.maxDuration, avg, inflateCount,
					acc.threads.size(), acc.addresses.size()));
		}
		results.sort(Comparator.comparingLong(LockHistogram::totalDuration).reversed());
		return JmcResultLimiter.limitRows(results);
	}

	private Map<String, Long> collectInflateCounts(IItemCollection events) {
		IItemCollection inflateEvents = events.apply(JdkFilters.MONITOR_INFLATE);
		Map<String, Long> counts = new HashMap<>();
		if (!inflateEvents.hasItems()) {
			return counts;
		}
		for (IItemIterable itemIterable : inflateEvents) {
			IMemberAccessor<IQuantity, IItem> addressAccessor =
					getAccessor(itemIterable, JdkAttributes.MONITOR_ADDRESS);
			for (IItem item : itemIterable) {
				if (addressAccessor != null) {
					IQuantity addrQty = addressAccessor.getMember(item);
					if (addrQty != null) {
						String address = addrQty.persistableString();
						counts.merge(address, 1L, Long::sum);
					}
				}
			}
		}
		return counts;
	}

	@SuppressWarnings("unchecked")
	private static <T> IMemberAccessor<T, IItem> getAccessor(
			IItemIterable itemIterable, IAttribute<T> attribute) {
		return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
	}

	private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

	private static final class LockAccumulator {
		final String key;
		long count;
		long totalDuration;
		long maxDuration;
		final Set<String> threads = new HashSet<>();
		final Set<String> addresses = new HashSet<>();

		LockAccumulator(String key) {
			this.key = key;
		}
	}
}
