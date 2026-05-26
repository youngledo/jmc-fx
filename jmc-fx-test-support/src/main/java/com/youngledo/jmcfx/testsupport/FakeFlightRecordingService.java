package com.youngledo.jmcfx.testsupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingState;
import com.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;

public class FakeFlightRecordingService implements FlightRecordingService {

    private final Set<String> availableConnections = new HashSet<>();
    private final Map<String, List<FlightRecordingInfo>> recordingsByConnectionId = new ConcurrentHashMap<>();
    private final AtomicLong nextRecordingId = new AtomicLong(1000);
    private RuntimeException failure;
    private FlightRecordingStartRequest lastStartRequest;
    private FlightRecordingStopRequest lastStopRequest;
    private final List<Long> discardedRecordingIds = new ArrayList<>();

    public void setAvailable(String connectionId, boolean available) {
        if (available) {
            availableConnections.add(connectionId);
        } else {
            availableConnections.remove(connectionId);
        }
    }

    public void addRecording(String connectionId, FlightRecordingInfo recording) {
        recordingsByConnectionId.computeIfAbsent(connectionId, ignored -> new ArrayList<>()).add(recording);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public FlightRecordingStartRequest lastStartRequest() {
        return lastStartRequest;
    }

    public FlightRecordingStopRequest lastStopRequest() {
        return lastStopRequest;
    }

    public List<Long> discardedRecordingIds() {
        return List.copyOf(discardedRecordingIds);
    }

    @Override
    public boolean isRecordingControlAvailable(JvmConnection connection) {
        failIfConfigured();
        return connection != null && availableConnections.contains(connection.id());
    }

    @Override
    public List<FlightRecordingTemplate> templates(JvmConnection connection) {
        failIfConfigured();
        return FlightRecordingTemplate.predefined();
    }

    @Override
    public List<FlightRecordingInfo> recordings(JvmConnection connection) {
        failIfConfigured();
        String id = connection == null ? "" : connection.id();
        return List.copyOf(recordingsByConnectionId.getOrDefault(id, List.of()));
    }

    @Override
    public FlightRecordingInfo startRecording(FlightRecordingStartRequest request) {
        failIfConfigured();
        lastStartRequest = request;
        FlightRecordingInfo recording = new FlightRecordingInfo(nextRecordingId.getAndIncrement(),
                request.name(), FlightRecordingState.RUNNING, 0, 0);
        addRecording(request.connection().id(), recording);
        return recording;
    }

    @Override
    public Path stopAndSaveRecording(FlightRecordingStopRequest request) {
        failIfConfigured();
        lastStopRequest = request;
        removeRecording(request.connection().id(), request.recordingId());
        return request.destinationFile();
    }

    @Override
    public void stopAndDiscardRecording(JvmConnection connection, long recordingId) {
        failIfConfigured();
        discardedRecordingIds.add(recordingId);
        removeRecording(connection.id(), recordingId);
    }

    private void failIfConfigured() {
        if (failure != null) {
            throw failure;
        }
    }

    private void removeRecording(String connectionId, long recordingId) {
        recordingsByConnectionId.computeIfPresent(connectionId, (ignored, recordings) -> recordings.stream()
                .filter(recording -> recording.id() != recordingId)
                .toList());
    }
}
