package com.youngledo.jmcfx.domain.service;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface FlightRecordingService {
    boolean isRecordingControlAvailable(JvmConnection connection);

    void startRecording(JvmConnection connection, String recordingName);

    Path stopAndSaveRecording(JvmConnection connection, String recordingName, Path destinationDirectory);
}
