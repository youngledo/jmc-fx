package com.youngledo.jmcfx.domain.model;

/// A single file I/O event from a flight recording.
///
/// @param eventType the event type identifier (e.g., "jdk.FileRead" or "jdk.FileWrite")
/// @param path      the file path
/// @param bytes     the number of bytes read or written
/// @param durationMillis the event duration in milliseconds
/// @param timestamp the event start time as epoch millis
/// @param threadName the thread that performed the I/O
public record FileIOEvent(
        String eventType,
        String path,
        long bytes,
        double durationMillis,
        long timestamp,
        String threadName) {
}
