package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.time.Instant;
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
import com.youngledo.jmcfx.domain.model.ChartXAxisType;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.ExceptionService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

/// JMC-backed exception analysis adapter.
///
/// Uses {@link JdkFilters#THROWABLES} to extract exception events and build
/// histograms and timeline charts from flight recordings.
public class JmcExceptionService implements ExceptionService {

	@Override
	public List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping) {
		IItemCollection events = loadEvents(recording);
		IItemCollection throwables = events.apply(JdkFilters.THROWABLES);
		if (!throwables.hasItems()) {
			return List.of();
		}

		Map<String, ExceptionAccumulator> buckets = new HashMap<>();
		for (IItemIterable itemIterable : throwables) {
			IMemberAccessor<String, IItem> classAccessor =
					getAccessor(itemIterable, JdkAttributes.EXCEPTION_THROWNCLASS_NAME);
			IMemberAccessor<String, IItem> messageAccessor =
					getAccessor(itemIterable, JdkAttributes.EXCEPTION_MESSAGE);
			for (IItem item : itemIterable) {
				String className = classAccessor != null ? classAccessor.getMember(item) : null;
				String message = messageAccessor != null ? messageAccessor.getMember(item) : null;
				String key = switch (grouping) {
					case BY_CLASS -> className != null ? className : "<unknown>";
					case BY_MESSAGE -> message != null ? message : "<no message>";
					case BY_CLASS_AND_MESSAGE ->
						(className != null ? className : "<unknown>")
								+ ": " + (message != null ? message : "<no message>");
				};
				buckets.computeIfAbsent(key, k ->
						new ExceptionAccumulator(k, className, message)).increment();
			}
		}

		int total = buckets.values().stream().mapToInt(a -> a.count).sum();
		List<ExceptionSummary> results = new ArrayList<>();
		for (ExceptionAccumulator acc : buckets.values()) {
			double pct = total > 0 ? (acc.count * 100.0) / total : 0;
			results.add(new ExceptionSummary(acc.key, acc.className, acc.message, acc.count, pct));
		}
		results.sort(Comparator.comparingInt(ExceptionSummary::count).reversed());
		return JmcResultLimiter.limitRows(results);
	}

	@Override
	public ChartDefinition loadTimeline(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection stats = events.apply(JdkFilters.THROWABLES_STATISTICS);
		if (!stats.hasItems()) {
			return new ChartDefinition("Time", "Count", ChartXAxisType.EPOCH_MILLIS, List.of());
		}

		List<ChartDataPoint> points = new ArrayList<>();
		for (IItemIterable itemIterable : stats) {
			IMemberAccessor<IQuantity, IItem> startAccessor =
					JfrAttributes.START_TIME.getAccessor(itemIterable.getType());
			IMemberAccessor<IQuantity, IItem> countAccessor =
					getAccessor(itemIterable, JdkAttributes.EXCEPTION_THROWABLES_COUNT);
			for (IItem item : itemIterable) {
				IQuantity startTime = startAccessor != null ? startAccessor.getMember(item) : null;
				IQuantity count = countAccessor != null ? countAccessor.getMember(item) : null;
				if (startTime != null && count != null) {
					Instant instant = UnitLookup.toDate(startTime).toInstant();
					double timeMs = instant.toEpochMilli();
					points.add(new ChartDataPoint(timeMs, count.doubleValue()));
				}
			}
		}
		points.sort(Comparator.comparingDouble(ChartDataPoint::x));

		ChartSeries series = new ChartSeries("exceptions", "Exceptions",
				ChartSeriesType.LINE, List.copyOf(points));
		return JmcResultLimiter.limitChart(new ChartDefinition("Time", "Count",
				ChartXAxisType.EPOCH_MILLIS, List.of(series)));
	}

	@SuppressWarnings("unchecked")
	private static <T> IMemberAccessor<T, IItem> getAccessor(
			IItemIterable itemIterable, IAttribute<T> attribute) {
		return (IMemberAccessor<T, IItem>) attribute.getAccessor(itemIterable.getType());
	}

	private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

	private static final class ExceptionAccumulator {
		final String key;
		final String className;
		final String message;
		int count;

		ExceptionAccumulator(String key, String className, String message) {
			this.key = key;
			this.className = className;
			this.message = message;
		}

		void increment() {
			count++;
		}
	}
}
