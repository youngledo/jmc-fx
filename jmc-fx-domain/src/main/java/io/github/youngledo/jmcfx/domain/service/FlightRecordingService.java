package io.github.youngledo.jmcfx.domain.service;

import java.nio.file.Path;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;

public interface FlightRecordingService {
    boolean isRecordingControlAvailable(JvmConnection connection);

    default List<FlightRecordingTemplate> templates(JvmConnection connection) {
        return FlightRecordingTemplate.predefined();
    }

    default List<FlightRecordingInfo> recordings(JvmConnection connection) {
        throw new JmcFxException("Recording listing is not supported by this service.");
    }

    default FlightRecordingInfo startRecording(FlightRecordingStartRequest request) {
        throw new JmcFxException("Recording control is not supported by this service.");
    }

    default Path stopAndSaveRecording(FlightRecordingStopRequest request) {
        throw new JmcFxException("Recording control is not supported by this service.");
    }

    default void stopAndDiscardRecording(JvmConnection connection, long recordingId) {
        throw new JmcFxException("Recording control is not supported by this service.");
    }

    default void startRecording(JvmConnection connection, String recordingName) {
        startRecording(new FlightRecordingStartRequest(connection, recordingName, FlightRecordingTemplate.profile()));
    }

    default Path stopAndSaveRecording(JvmConnection connection, String recordingName, Path destinationDirectory) {
        Path destinationFile = destinationDirectory.resolve(recordingName + ".jfr");
        return stopAndSaveRecording(new FlightRecordingStopRequest(connection, -1, destinationFile));
    }
}
