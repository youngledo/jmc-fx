package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.internal.ChunkInfo;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;

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
            RecordingTimeRange timeRange = recordingTimeRange(normalized);
            return new RecordingSummary(normalized.toString(), path, path.getFileName().toString(),
                    timeRange.startTime(), timeRange.endTime(), timeRange.durationMillis(), size);
        } catch (IOException exception) {
            throw new JmcFxException("Unable to read recording file: " + path, exception);
        }
    }

    private RecordingTimeRange recordingTimeRange(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            List<ChunkInfo> chunks = FlightRecordingLoader.readChunkInfo(
                    FlightRecordingLoader.createChunkSupplier(file));
            if (chunks.isEmpty()) {
                return RecordingTimeRange.EMPTY;
            }
            Instant start = null;
            Instant end = null;
            for (ChunkInfo chunk : chunks) {
                IRange<IQuantity> range = chunk.getChunkRange();
                if (range == null) {
                    continue;
                }
                Instant chunkStart = toInstant(range.getStart());
                Instant chunkEnd = toInstant(range.getEnd());
                if (chunkStart != null && (start == null || chunkStart.isBefore(start))) {
                    start = chunkStart;
                }
                if (chunkEnd != null && (end == null || chunkEnd.isAfter(end))) {
                    end = chunkEnd;
                }
            }
            if (start == null || end == null || end.isBefore(start)) {
                return RecordingTimeRange.EMPTY;
            }
            return new RecordingTimeRange(start, end, java.time.Duration.between(start, end).toMillis());
        } catch (CouldNotLoadRecordingException exception) {
            return RecordingTimeRange.EMPTY;
        }
    }

    private static Instant toInstant(IQuantity quantity) {
        return quantity == null ? null : UnitLookup.toDate(quantity).toInstant();
    }

    private record RecordingTimeRange(Instant startTime, Instant endTime, long durationMillis) {
        private static final RecordingTimeRange EMPTY = new RecordingTimeRange(Instant.EPOCH, Instant.EPOCH, 0);
    }
}
