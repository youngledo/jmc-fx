package io.github.youngledo.jmcfx.domain.model;

import java.nio.file.Path;
import java.util.Objects;

public record FlightRecordingStopRequest(
        JvmConnection connection,
        long recordingId,
        Path destinationFile) {

    public FlightRecordingStopRequest {
        connection = Objects.requireNonNull(connection, "connection");
        destinationFile = Objects.requireNonNull(destinationFile, "destinationFile");
    }
}
