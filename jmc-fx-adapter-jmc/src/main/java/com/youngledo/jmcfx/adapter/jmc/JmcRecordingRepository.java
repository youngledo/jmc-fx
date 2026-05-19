package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.RecordingRepository;

/// JMC-backed implementation of the `RecordingRepository` port.
///
/// This adapter is the boundary where OpenJDK JMC recording APIs are translated
/// into UI-neutral domain records and exceptions.
public class JmcRecordingRepository implements RecordingRepository {

    @Override
    public RecordingSummary open(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new JmcFxException("Recording file does not exist: " + path);
        }
        try {
            long size = Files.size(path);
            Path normalized = path.toAbsolutePath().normalize();
            TimeRange range = readTimeRange(path);
            return new RecordingSummary(normalized.toString(), path, path.getFileName().toString(),
                    range.start, range.end, range.durationMillis, size);
        } catch (IOException exception) {
            throw new JmcFxException("Unable to read recording file: " + path, exception);
        }
    }

    private static TimeRange readTimeRange(Path path) {
        try {
            IItemCollection events = JfrLoaderToolkit.loadEvents(path.toFile());
            return computeTimeRange(events);
        } catch (CouldNotLoadRecordingException | IOException exception) {
            return new TimeRange(Instant.EPOCH, Instant.EPOCH, 0);
        }
    }

    private static TimeRange computeTimeRange(IItemCollection events) {
        IQuantity minStart = null;
        IQuantity maxEnd = null;
        for (IItemIterable itemIter : events) {
            IMemberAccessor<IQuantity, IItem> startAccessor =
                    JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> endAccessor =
                    JfrAttributes.END_TIME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                IQuantity start = startAccessor.getMember(item);
                IQuantity end = endAccessor.getMember(item);
                if (start != null && (minStart == null || start.compareTo(minStart) < 0)) {
                    minStart = start;
                }
                if (end != null && (maxEnd == null || end.compareTo(maxEnd) > 0)) {
                    maxEnd = end;
                }
            }
        }
        Instant startInstant = toInstant(minStart);
        Instant endInstant = toInstant(maxEnd);
        long durationMillis = startInstant.equals(Instant.EPOCH) || endInstant.equals(Instant.EPOCH)
                ? 0
                : endInstant.toEpochMilli() - startInstant.toEpochMilli();
        return new TimeRange(startInstant, endInstant, durationMillis);
    }

    private static Instant toInstant(IQuantity quantity) {
        if (quantity == null) {
            return Instant.EPOCH;
        }
        return UnitLookup.toDate(quantity).toInstant();
    }

    private record TimeRange(Instant start, Instant end, long durationMillis) {
    }
}
