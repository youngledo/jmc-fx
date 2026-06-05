package io.github.youngledo.jmcfx.domain.model;

public record ActiveRecordingInfo(
        String id,
        String name,
        String destination,
        long maxAgeMillis,
        long maxSizeBytes,
        String startTime,
        String recordingDuration,
        long eventCount) {
}
