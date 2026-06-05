package io.github.youngledo.jmcfx.ui.recording;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import javafx.collections.ObservableList;

/// Shared helpers for filtering timestamped recording rows by the workspace time range.
public final class RecordingTimeRangeFilters {

    private RecordingTimeRangeFilters() {
    }

    public static <T> void apply(ObservableList<T> target, List<T> source,
            RecordingTimeRange range, Function<T, Instant> timestamp) {
        if (range == null) {
            target.setAll(source);
            return;
        }
        target.setAll(source.stream()
                .filter(row -> range.contains(timestamp.apply(row)))
                .toList());
    }
}
