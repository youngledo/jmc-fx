package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IAttribute;
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
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadActivity;
import com.youngledo.jmcfx.domain.model.ThreadLaneType;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.ThreadService;

/// JMC-backed thread activity adapter.
///
/// Uses {@link JdkFilters#THREAD_LATENCIES} and {@link JdkFilters#EXECUTION_SAMPLE}
/// to extract thread timelines from flight recordings. Thread names are resolved
/// via {@link JdkAttributes#EVENT_THREAD_NAME} and
/// {@link JfrAttributes#EVENT_THREAD}.
public class JmcThreadService implements ThreadService {

	@Override
	public List<ThreadSummary> loadThreadSummaries(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection latencies = events.apply(JdkFilters.THREAD_LATENCIES);
		IItemCollection samples = events.apply(JdkFilters.EXECUTION_SAMPLE);

		Map<String, ThreadAccumulator> buckets = new HashMap<>();

		// Count execution samples per thread
		for (IItemIterable itemIter : samples) {
			for (var item : itemIter) {
				ThreadInfo info = resolveThreadInfo(item, itemIter);
				if (info != null && info.name != null) {
					buckets.computeIfAbsent(info.name,
							k -> new ThreadAccumulator(info)).sampleCount++;
				}
			}
		}

		// Collect latency activities per thread
		for (IItemIterable itemIter : latencies) {
			for (var item : itemIter) {
				ThreadInfo info = resolveThreadInfo(item, itemIter);
				if (info == null || info.name == null) {
					continue;
				}
				ThreadAccumulator acc = buckets.computeIfAbsent(info.name,
						k -> new ThreadAccumulator(info));
				ThreadLaneType lane = classifyLane(itemIter.getType().getIdentifier());
				if (lane == null) {
					continue;
				}
				long startMs = getTimestampMillis(item, itemIter);
				long durationMs = getDurationMillis(item, itemIter);
				acc.activities.add(new ThreadActivity(lane, startMs, startMs + durationMs, ""));
				if (lane == ThreadLaneType.BLOCKED) {
					acc.blockedDurationMillis += durationMs;
				}
			}
		}

		List<ThreadSummary> results = new ArrayList<>();
		for (ThreadAccumulator acc : buckets.values()) {
			results.add(new ThreadSummary(acc.info.name, acc.info.id, acc.info.group,
					false, acc.sampleCount, acc.blockedDurationMillis,
					List.copyOf(acc.activities)));
		}
		results.sort(Comparator.comparingInt(ThreadSummary::sampleCount).reversed());
		return List.copyOf(results);
	}

	private ThreadLaneType classifyLane(String eventTypeId) {
		if (eventTypeId == null) {
			return null;
		}
		return switch (eventTypeId) {
			case JdkTypeIDs.EXECUTION_SAMPLE -> ThreadLaneType.CPU_SAMPLE;
			case JdkTypeIDs.MONITOR_ENTER -> ThreadLaneType.BLOCKED;
			case JdkTypeIDs.THREAD_PARK -> ThreadLaneType.PARKED;
			case JdkTypeIDs.THREAD_SLEEP -> ThreadLaneType.SLEEPING;
			case JdkTypeIDs.SOCKET_READ, JdkTypeIDs.SOCKET_WRITE -> ThreadLaneType.SOCKET_IO;
			case JdkTypeIDs.FILE_READ, JdkTypeIDs.FILE_WRITE -> ThreadLaneType.FILE_IO;
			case JdkTypeIDs.COMPILATION -> ThreadLaneType.COMPILATION;
			case JdkTypeIDs.CLASS_LOAD -> ThreadLaneType.CLASS_LOAD;
			default -> null;
		};
	}

	private static ThreadInfo resolveThreadInfo(
			org.openjdk.jmc.common.item.IItem item, IItemIterable itemIter) {
		// Prefer JdkAttributes.EVENT_THREAD_NAME for direct name access
		IMemberAccessor<String, org.openjdk.jmc.common.item.IItem> nameAccessor =
				JdkAttributes.EVENT_THREAD_NAME.getAccessor(itemIter.getType());
		String name = nameAccessor.getMember(item);
		if (name == null) {
			return null;
		}

		long threadId = 0;
		String group = "";

		// Get thread ID via IMCThread
		IMemberAccessor<IMCThread, org.openjdk.jmc.common.item.IItem> threadAccessor =
				JfrAttributes.EVENT_THREAD.getAccessor(itemIter.getType());
		IMCThread thread = threadAccessor.getMember(item);
		if (thread != null) {
			if (thread.getThreadId() != null) {
				threadId = thread.getThreadId();
			}
			if (thread.getThreadGroup() != null) {
				String groupName = thread.getThreadGroup().getName();
				if (groupName != null) {
					group = groupName;
				}
			}
		}

		return new ThreadInfo(name, threadId, group);
	}

	private static long getTimestampMillis(
			org.openjdk.jmc.common.item.IItem item, IItemIterable itemIter) {
		IMemberAccessor<IQuantity, org.openjdk.jmc.common.item.IItem> accessor =
				((IAttribute<IQuantity>) JfrAttributes.START_TIME).getAccessor(itemIter.getType());
		IQuantity time = accessor.getMember(item);
		if (time == null) {
			return 0;
		}
		try {
			return time.longValueIn(UnitLookup.EPOCH_MS);
		} catch (org.openjdk.jmc.common.unit.QuantityConversionException e) {
			return 0;
		}
	}

	private static long getDurationMillis(
			org.openjdk.jmc.common.item.IItem item, IItemIterable itemIter) {
		IMemberAccessor<IQuantity, org.openjdk.jmc.common.item.IItem> accessor =
				((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());
		IQuantity duration = accessor.getMember(item);
		if (duration == null) {
			return 0;
		}
		try {
			return duration.longValueIn(UnitLookup.MILLISECOND);
		} catch (org.openjdk.jmc.common.unit.QuantityConversionException e) {
			return 0;
		}
	}

	private IItemCollection loadEvents(RecordingSummary recording) {
		try {
			return JfrLoaderToolkit.loadEvents(recording.path().toFile());
		} catch (IOException | CouldNotLoadRecordingException e) {
			throw new JmcFxException(
					"Unable to load recording for thread analysis: " + recording.path(), e);
		}
	}

	private record ThreadInfo(String name, long id, String group) {
	}

	private static final class ThreadAccumulator {
		final ThreadInfo info;
		int sampleCount;
		long blockedDurationMillis;
		final List<ThreadActivity> activities = new ArrayList<>();

		ThreadAccumulator(ThreadInfo info) {
			this.info = info;
		}
	}
}
