package io.github.youngledo.jmcfx.ui.recording;

/// A workspace-level selected time range for timestamped recording pages.
public record RecordingTimeRange(long startEpochMillis, long endEpochMillis) {

    public RecordingTimeRange {
        if (endEpochMillis < startEpochMillis) {
            throw new IllegalArgumentException("endEpochMillis must be >= startEpochMillis");
        }
    }

    public boolean contains(long epochMillis) {
        return epochMillis >= startEpochMillis && epochMillis <= endEpochMillis;
    }

    public long durationMillis() {
        return endEpochMillis - startEpochMillis;
    }

    public static RecordingTimeRange fromBounds(double lowerBound, double upperBound) {
        if (!Double.isFinite(lowerBound) || !Double.isFinite(upperBound)) {
            throw new IllegalArgumentException("range bounds must be finite");
        }
        long start = Math.round(Math.min(lowerBound, upperBound));
        long end = Math.round(Math.max(lowerBound, upperBound));
        return new RecordingTimeRange(start, end);
    }
}
