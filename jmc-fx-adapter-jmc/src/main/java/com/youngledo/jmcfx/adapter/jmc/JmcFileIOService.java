package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
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
import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.FileIOService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

/// JMC-backed file I/O analysis adapter.
///
/// Uses {@link JdkFilters#FILE_READ} and {@link JdkFilters#FILE_WRITE} to
/// extract file I/O events, aggregating by {@link JdkAttributes#IO_PATH} for
/// the histogram. Timeline data is sourced from {@link JdkFilters#FILE_OR_SOCKET_IO}.
/// Duration is converted to milliseconds via {@link UnitLookup#MILLISECOND}.
public class JmcFileIOService implements FileIOService {

	@Override
	public List<FileIOHistogram> loadFileIOHistogram(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection fileReads = events.apply(JdkFilters.FILE_READ);
		IItemCollection fileWrites = events.apply(JdkFilters.FILE_WRITE);

		Map<String, FileIOAccumulator> buckets = new HashMap<>();
		collectHistogramEntries(fileReads, buckets, true);
		collectHistogramEntries(fileWrites, buckets, false);

		List<FileIOHistogram> results = new ArrayList<>();
		for (FileIOAccumulator acc : buckets.values()) {
			double avgDuration = acc.count > 0 ? (double) acc.totalDuration / acc.count : 0;
			results.add(new FileIOHistogram(acc.path, acc.readCount, acc.writeCount,
					acc.readSize, acc.writeSize, acc.totalDuration, acc.maxDuration, avgDuration));
		}
		results.sort(Comparator.comparingLong(FileIOHistogram::totalDuration).reversed());
		return JmcResultLimiter.limitRows(results);
	}

	@Override
	public List<FileIOEvent> loadFileIOEvents(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection fileReads = events.apply(JdkFilters.FILE_READ);
		IItemCollection fileWrites = events.apply(JdkFilters.FILE_WRITE);

		List<FileIOEvent> results = new ArrayList<>();
		collectEvents(fileReads, results, "jdk.FileRead");
		collectEvents(fileWrites, results, "jdk.FileWrite");
		results.sort(Comparator.comparingLong(FileIOEvent::timestamp));
		return JmcResultLimiter.limitRows(results);
	}

	@Override
	public ChartDefinition loadTimeline(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection ioEvents = events.apply(JdkFilters.FILE_OR_SOCKET_IO);
		if (!ioEvents.hasItems()) {
			return new ChartDefinition("Time", "Duration (ms)", List.of());
		}

		List<ChartDataPoint> readPoints = new ArrayList<>();
		List<ChartDataPoint> writePoints = new ArrayList<>();

		for (IItemIterable itemIter : ioEvents) {
			IMemberAccessor<IQuantity, IItem> startAccessor =
					((IAttribute<IQuantity>) JfrAttributes.START_TIME).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> bytesReadAccessor =
					getAccessor(itemIter, JdkAttributes.IO_FILE_BYTES_READ);
			IMemberAccessor<IQuantity, IItem> bytesWrittenAccessor =
					getAccessor(itemIter, JdkAttributes.IO_FILE_BYTES_WRITTEN);

			for (IItem item : itemIter) {
				long timestamp = getTimestampMillis(item, startAccessor);
				double duration = getDurationMillisDouble(item, durationAccessor);

				IQuantity bytesRead = bytesReadAccessor != null ? bytesReadAccessor.getMember(item) : null;
				IQuantity bytesWritten = bytesWrittenAccessor != null ? bytesWrittenAccessor.getMember(item) : null;

				if (bytesRead != null && bytesRead.doubleValue() > 0) {
					readPoints.add(new ChartDataPoint(timestamp, duration));
				}
				if (bytesWritten != null && bytesWritten.doubleValue() > 0) {
					writePoints.add(new ChartDataPoint(timestamp, duration));
				}
			}
		}

		readPoints.sort(Comparator.comparingDouble(ChartDataPoint::x));
		writePoints.sort(Comparator.comparingDouble(ChartDataPoint::x));

		List<ChartSeries> seriesList = new ArrayList<>();
		if (!readPoints.isEmpty()) {
			seriesList.add(new ChartSeries("file-read", "File Read",
					ChartSeriesType.LINE, List.copyOf(readPoints)));
		}
		if (!writePoints.isEmpty()) {
			seriesList.add(new ChartSeries("file-write", "File Write",
					ChartSeriesType.LINE, List.copyOf(writePoints)));
		}
		return JmcResultLimiter.limitChart(new ChartDefinition("Time", "Duration (ms)", List.copyOf(seriesList)));
	}

	private void collectHistogramEntries(IItemCollection collection,
			Map<String, FileIOAccumulator> buckets, boolean isRead) {
		for (IItemIterable itemIter : collection) {
			IMemberAccessor<String, IItem> pathAccessor =
					getAccessor(itemIter, JdkAttributes.IO_PATH);
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> bytesAccessor = isRead
					? getAccessor(itemIter, JdkAttributes.IO_FILE_BYTES_READ)
					: getAccessor(itemIter, JdkAttributes.IO_FILE_BYTES_WRITTEN);

			for (IItem item : itemIter) {
				String path = pathAccessor != null ? pathAccessor.getMember(item) : "<unknown>";
				long duration = getDurationMillis(item, durationAccessor);
				long bytes = 0;
				if (bytesAccessor != null) {
					IQuantity bytesQuantity = bytesAccessor.getMember(item);
					if (bytesQuantity != null) {
						bytes = bytesQuantity.longValue();
					}
				}

				FileIOAccumulator acc = buckets.computeIfAbsent(path, FileIOAccumulator::new);
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

	private void collectEvents(IItemCollection collection, List<FileIOEvent> results, String eventType) {
		for (IItemIterable itemIter : collection) {
			IMemberAccessor<String, IItem> pathAccessor =
					getAccessor(itemIter, JdkAttributes.IO_PATH);
			IMemberAccessor<IQuantity, IItem> durationAccessor =
					((IAttribute<IQuantity>) JfrAttributes.DURATION).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> startAccessor =
					((IAttribute<IQuantity>) JfrAttributes.START_TIME).getAccessor(itemIter.getType());
			IMemberAccessor<IQuantity, IItem> bytesAccessor =
					"jdk.FileRead".equals(eventType)
							? getAccessor(itemIter, JdkAttributes.IO_FILE_BYTES_READ)
							: getAccessor(itemIter, JdkAttributes.IO_FILE_BYTES_WRITTEN);
			IMemberAccessor<String, IItem> threadAccessor =
					getAccessor(itemIter, JdkAttributes.EVENT_THREAD_NAME);

			for (IItem item : itemIter) {
				String path = pathAccessor != null ? pathAccessor.getMember(item) : "<unknown>";
				double duration = getDurationMillisDouble(item, durationAccessor);
				long timestamp = getTimestampMillis(item, startAccessor);
				long bytes = 0;
				if (bytesAccessor != null) {
					IQuantity bytesQuantity = bytesAccessor.getMember(item);
					if (bytesQuantity != null) {
						bytes = bytesQuantity.longValue();
					}
				}
				String threadName = threadAccessor != null ? threadAccessor.getMember(item) : "<unknown>";
				results.add(new FileIOEvent(eventType, path, bytes, duration, timestamp, threadName));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> IMemberAccessor<T, IItem> getAccessor(
			IItemIterable itemIterable, IAttribute<T> attribute) {
		return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
	}

	private static long getDurationMillis(IItem item, IMemberAccessor<IQuantity, IItem> accessor) {
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

	private static double getDurationMillisDouble(IItem item, IMemberAccessor<IQuantity, IItem> accessor) {
		if (accessor == null) {
			return 0;
		}
		IQuantity duration = accessor.getMember(item);
		if (duration == null) {
			return 0;
		}
		return duration.doubleValueIn(UnitLookup.MILLISECOND);
	}

	private static long getTimestampMillis(IItem item, IMemberAccessor<IQuantity, IItem> accessor) {
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

	private static final class FileIOAccumulator {
		final String path;
		long readCount;
		long writeCount;
		long readSize;
		long writeSize;
		long totalDuration;
		long maxDuration;
		int count;

		FileIOAccumulator(String path) {
			this.path = path;
		}
	}
}
