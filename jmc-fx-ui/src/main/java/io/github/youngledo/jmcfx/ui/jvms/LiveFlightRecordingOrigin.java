package io.github.youngledo.jmcfx.ui.jvms;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.JvmConnectionSource;

/// Describes the live JVM source that produced an offline JFR workspace.
public record LiveFlightRecordingOrigin(
        String connectionId,
        String displayName,
        String connectionUrl,
        JvmConnectionSource source,
        String pid,
        String javaVersion,
        long recordingId,
        String recordingName) {

    public LiveFlightRecordingOrigin {
        connectionId = Objects.requireNonNullElse(connectionId, "");
        displayName = Objects.requireNonNullElse(displayName, "");
        connectionUrl = Objects.requireNonNullElse(connectionUrl, "");
        source = source == null ? JvmConnectionSource.MANUAL : source;
        pid = Objects.requireNonNullElse(pid, "");
        javaVersion = Objects.requireNonNullElse(javaVersion, "");
        recordingName = Objects.requireNonNullElse(recordingName, "");
    }

    public static LiveFlightRecordingOrigin from(JvmConnection connection, FlightRecordingInfo recording) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(recording, "recording");
        return new LiveFlightRecordingOrigin(connection.id(), connection.displayName(), connection.connectionUrl(),
                connection.source(), connection.pid(), connection.javaVersion(), recording.id(), recording.name());
    }
}
