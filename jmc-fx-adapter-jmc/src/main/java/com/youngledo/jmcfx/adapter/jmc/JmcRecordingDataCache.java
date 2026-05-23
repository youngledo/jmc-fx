package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemIterableToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;

final class JmcRecordingDataCache {

    static final JmcRecordingDataCache SHARED = new JmcRecordingDataCache();

    private final ConcurrentMap<Path, RecordingData> recordings = new ConcurrentHashMap<>();

    IItemCollection events(RecordingSummary recording) {
        return recording(recording).events();
    }

    RecordingData recording(RecordingSummary recording) {
        Path path = recording.path().toAbsolutePath().normalize();
        return recordings.computeIfAbsent(path, ignored -> load(recording, path));
    }

    private RecordingData load(RecordingSummary recording, Path path) {
        try (InputStream input = java.nio.file.Files.newInputStream(path)) {
            EventArrays eventArrays = FlightRecordingLoader.loadStream(input, false, true);
            IItemCollection events = toItemCollection(eventArrays);
            return new RecordingData(events, eventArrays);
        } catch (IOException | CouldNotLoadRecordingException exception) {
            throw new JmcFxException("Unable to load recording data: " + recording.path(), exception);
        }
    }

    private static IItemCollection toItemCollection(EventArrays eventArrays) {
        EventArray[] eventsByType = eventArrays.getArrays();
        return ItemCollectionToolkit.build(
                () -> Arrays.stream(eventsByType)
                        .map(eventArray -> ItemIterableToolkit.build(
                                () -> Arrays.stream(eventArray.getEvents()),
                                eventArray.getType())),
                eventArrays.getChunkTimeranges());
    }

    record RecordingData(IItemCollection events, EventArrays eventArrays) {
    }
}
