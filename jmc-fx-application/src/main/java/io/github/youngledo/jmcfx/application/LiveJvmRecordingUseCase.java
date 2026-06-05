package io.github.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.FlightRecordingService;

public final class LiveJvmRecordingUseCase {

    private final FlightRecordingService service;

    public LiveJvmRecordingUseCase(FlightRecordingService service) {
        this.service = service;
    }

    public boolean available() {
        return service != null;
    }

    public boolean isRecordingControlAvailable(JvmConnection connection) {
        return service.isRecordingControlAvailable(connection);
    }

    public List<FlightRecordingTemplate> templates(JvmConnection connection) {
        return service.templates(connection);
    }

    public List<FlightRecordingInfo> recordings(JvmConnection connection) {
        return service.recordings(connection);
    }

    public FlightRecordingInfo startRecording(FlightRecordingStartRequest request) {
        return service.startRecording(request);
    }

    public Path stopAndSaveRecording(FlightRecordingStopRequest request) {
        return service.stopAndSaveRecording(request);
    }

    public void stopAndDiscardRecording(JvmConnection connection, long recordingId) {
        service.stopAndDiscardRecording(connection, recordingId);
    }
}
