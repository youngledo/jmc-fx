package io.github.youngledo.jmcfx.ui.jvms;

import java.nio.file.Path;
import java.util.Objects;

/// Flight Recording file saved from a live JVM session, with enough context for
/// the shell to connect the resulting offline workspace back to its source.
public record SavedFlightRecording(Path path, LiveFlightRecordingOrigin origin) {

    public SavedFlightRecording {
        path = Objects.requireNonNull(path, "path");
        origin = Objects.requireNonNull(origin, "origin");
    }
}
