package com.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

/// JMC-backed flight recording control boundary.
///
/// Recording control is intentionally unavailable until the standalone JMC
/// management API path is verified.
public class JmcFlightRecordingService implements FlightRecordingService {

    @Override
    public boolean isRecordingControlAvailable(JvmConnection connection) {
        return false;
    }

    @Override
    public void startRecording(JvmConnection connection, String recordingName) {
        throw new JmcFxException("Recording control is not available yet.");
    }

    @Override
    public Path stopAndSaveRecording(JvmConnection connection, String recordingName, Path destinationDirectory) {
        throw new JmcFxException("Recording control is not available yet.");
    }
}
