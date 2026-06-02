package com.youngledo.jmcfx.adapter.jmc;

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
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;

import com.youngledo.jmcfx.domain.model.ChartDataPoint;
import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;
import com.youngledo.jmcfx.domain.model.ChartSeriesType;
import com.youngledo.jmcfx.domain.model.ChartXAxisType;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.SocketIOService;

/// JMC-backed socket I/O analysis adapter.
///
/// Uses {@link JdkFilters#SOCKET_READ} and {@link JdkFilters#SOCKET_WRITE} to
/// extract socket I/O events, aggregating by host and/or port for the histogram
/// based on the {@link SocketIOGrouping} strategy. Timeline data provides
/// separate series for reads and writes. Duration is converted to milliseconds
/// via {@link UnitLookup#MILLISECOND}.
public class JmcSocketIOService implements SocketIOService {

	@Override
	public List<SocketIOHistogram> loadSocketIOHistogram(RecordingSummary recording,
			SocketIOGrouping grouping) {
		IItemCollection events = loadEvents(recording);
		IItemCollection socketReads = events.apply(JdkFilters.SOCKET_READ);
		IItemCollection socketWrites = events.apply(JdkFilters.SOCKET_WRITE);

		Map<String, SocketIOAccumulator> buckets = new HashMap<>();
		collectHistogramEntries(socketReads, buckets, grouping, true);
		collectHistogramEntries(socketWrites, buckets, grouping, false);

		List<SocketIOHistogram> results = new ArrayList<>();
		for (SocketIOAccumulator acc : buckets.values()) {
			double avgDuration = acc.count > 0 ? (double) acc.totalDuration / acc.count : 0;
			results.add(new SocketIOHistogram(acc.key, acc.host, acc.port,
					acc.readCount, acc.writeCount, acc.readSize, acc.writeSize,
					acc.totalDuration, acc.maxDuration, avgDuration));
		}
		results.sort(Comparator.comparingLong(SocketIOHistogram::totalDuration).reversed());
		return JmcResultLimiter.limitRows(results);
	}

	@Override
	public List<SocketIOEvent> loadSocketIOEvents(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection socketReads = events.apply(JdkFilters.SOCKET_READ);
		IItemCollection socketWrites = events.apply(JdkFilters.SOCKET_WRITE);

		List<SocketIOEvent> results = new ArrayList<>();
		collectEvents(socketReads, results, "jdk.SocketRead");
		collectEvents(socketWrites, results, "jdk.SocketWrite");
		results.sort(Comparator.comparingLong(SocketIOEvent::timestamp));
		return JmcResultLimiter.limitRows(results);
	}

	@Override
	public ChartDefinition loadTimeline(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection socketReads = events.apply(JdkFilters.SOCKET_READ);
		IItemCollection socketWrites = events.apply(JdkFilters.SOCKET_WRITE);
		if (!socketReads.hasItems() && !socketWrites.hasItems()) {
			return new ChartDefinition("Time", "Duration (ms)", ChartXAxisType.EPOCH_MILLIS, List.of());
		}

		List<ChartDataPoint> readPoints = new ArrayList<>();
		List<ChartDataPoint> writePoints = new ArrayList<>();

		collectTimelinePoints(socketReads, readPoints);
		collectTimelinePoints(socketWrites, writePoints);

		readPoints.sort(Comparator.comparingDouble(ChartDataPoint::x));
		writePoints.sort(Comparator.comparingDouble(ChartDataPoint::x));

		List<ChartSeries> seriesList = new ArrayList<>();
		if (!readPoints.isEmpty()) {
			seriesList.add(new ChartSeries("socket-read", "Socket Read",
					ChartSeriesType.LINE, List.copyOf(readPoints)));
		}
		if (!writePoints.isEmpty()) {
			seriesList.add(new ChartSeries("socket-write", "Socket Write",
					ChartSeriesType.LINE, List.copyOf(writePoints)));
		}
		return JmcResultLimiter.limitChart(new ChartDefinition("Time", "Duration (ms)",
				ChartXAxisType.EPOCH_MILLIS, List.copyOf(seriesList)));
	}

	private void collectHistogramEntries(IItemCollection collection,
			Map<String, SocketIOAccumulator> buckets, SocketIOGrouping grouping,
			boolean isRead) {
		for (IItemIterable itemIter : collection) {
			IMemberAccessor<String, IItem> hostAccessor =
					getAccessor(itemIter, JdkAttributes.IO_HOST);
			IMemberAccessor<IQuantity, IItem> portAccessor =
					getAccessor(itemIter, JdkAttributes.IO_PORT);
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> bytesAccessor = isRead
					? getAccessor(itemIter, JdkAttributes.IO_SOCKET_BYTES_READ)
					: getAccessor(itemIter, JdkAttributes.IO_SOCKET_BYTES_WRITTEN);

			for (IItem item : itemIter) {
				String rawHost = hostAccessor != null ? hostAccessor.getMember(item) : null;
				String host = rawHost != null ? rawHost : "<unknown>";
				long port = getPortValue(item, portAccessor);
				long duration = getDurationMillis(item, durationAccessor);
				long bytes = getQuantityLongValue(item, bytesAccessor);

				String key = switch (grouping) {
					case BY_HOST -> host;
					case BY_PORT -> String.valueOf(port);
					case BY_HOST_AND_PORT -> host + ":" + port;
				};

				String finalHost = host;
				SocketIOAccumulator acc = buckets.computeIfAbsent(key,
						k -> new SocketIOAccumulator(k, finalHost, port));
				if (isRead) {
					acc.readCount++;
					acc.readSize += bytes;
				} else {
					acc.writeCount++;
					acc.writeSize += bytes;
				}
				acc.totalDuration += duration;
				acc.maxDuration = Math.max(acc.maxDuration, duration);
				acc.count++;
			}
		}
	}

	private void collectEvents(IItemCollection collection, List<SocketIOEvent> results,
			String eventType) {
		for (IItemIterable itemIter : collection) {
			IMemberAccessor<String, IItem> hostAccessor =
					getAccessor(itemIter, JdkAttributes.IO_HOST);
			IMemberAccessor<IQuantity, IItem> portAccessor =
					getAccessor(itemIter, JdkAttributes.IO_PORT);
			IMemberAccessor<IQuantity, IItem> timeoutAccessor =
					getAccessor(itemIter, JdkAttributes.IO_TIMEOUT);
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> startAccessor =
					((IAttribute<IQuantity>) JfrAttributes.START_TIME).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> bytesAccessor =
					"jdk.SocketRead".equals(eventType)
							? getAccessor(itemIter, JdkAttributes.IO_SOCKET_BYTES_READ)
							: getAccessor(itemIter, JdkAttributes.IO_SOCKET_BYTES_WRITTEN);
			IMemberAccessor<String, IItem> threadAccessor =
					getAccessor(itemIter, JdkAttributes.EVENT_THREAD_NAME);

			for (IItem item : itemIter) {
				String host = hostAccessor != null ? hostAccessor.getMember(item) : "<unknown>";
				long port = getPortValue(item, portAccessor);
				long bytes = getQuantityLongValue(item, bytesAccessor);
				long timeout = getQuantityLongValue(item, timeoutAccessor);
				double duration = getDurationMillisDouble(item, durationAccessor);
				long timestamp = getTimestampMillis(item, startAccessor);
				String threadName = threadAccessor != null
						? threadAccessor.getMember(item) : "<unknown>";
				results.add(new SocketIOEvent(eventType, host, port, bytes, timeout,
						duration, timestamp, threadName));
			}
		}
	}

	private void collectTimelinePoints(IItemCollection collection,
			List<ChartDataPoint> points) {
		for (IItemIterable itemIter : collection) {
			IMemberAccessor<IQuantity, IItem> startAccessor =
					((IAttribute<IQuantity>) JfrAttributes.START_TIME).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());

			for (IItem item : itemIter) {
				long timestamp = getTimestampMillis(item, startAccessor);
				double duration = getDurationMillisDouble(item, durationAccessor);
				points.add(new ChartDataPoint(timestamp, duration));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> IMemberAccessor<T, IItem> getAccessor(
			IItemIterable itemIterable, IAttribute<T> attribute) {
		return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
	}

	private static long getPortValue(IItem item, IMemberAccessor<IQuantity, IItem> accessor) {
		if (accessor == null) {
			return -1;
		}
		IQuantity port = accessor.getMember(item);
		if (port == null) {
			return -1;
		}
		return port.longValue();
	}

	private static long getQuantityLongValue(IItem item,
			IMemberAccessor<IQuantity, IItem> accessor) {
		if (accessor == null) {
			return 0;
		}
		IQuantity value = accessor.getMember(item);
		if (value == null) {
			return 0;
		}
		return value.longValue();
	}

	private static long getDurationMillis(IItem item,
			IMemberAccessor<IQuantity, IItem> accessor) {
		if (accessor == null) {
			return 0;
		}
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

	private static double getDurationMillisDouble(IItem item,
			IMemberAccessor<IQuantity, IItem> accessor) {
		if (accessor == null) {
			return 0;
		}
		IQuantity duration = accessor.getMember(item);
		if (duration == null) {
			return 0;
		}
		return duration.doubleValueIn(UnitLookup.MILLISECOND);
	}

	private static long getTimestampMillis(IItem item,
			IMemberAccessor<IQuantity, IItem> accessor) {
		if (accessor == null) {
			return 0;
		}
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

	private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

	private static final class SocketIOAccumulator {
		final String key;
		final String host;
		final long port;
		long readCount;
		long writeCount;
		long readSize;
		long writeSize;
		long totalDuration;
		long maxDuration;
		int count;

		SocketIOAccumulator(String key, String host, long port) {
			this.key = key;
			this.host = host;
			this.port = port;
		}
	}
}
