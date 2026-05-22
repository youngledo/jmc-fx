package com.youngledo.jmcfx.domain.model;

/// A single socket I/O event from a flight recording.
///
/// @param eventType the event type identifier (e.g., "jdk.SocketRead" or "jdk.SocketWrite")
/// @param host      the remote host address
/// @param port      the remote port number
/// @param bytes     the number of bytes read or written
/// @param timeout   the timeout value in milliseconds, or 0 if none
/// @param durationMillis the event duration in milliseconds
/// @param timestamp the event start time as epoch millis
/// @param threadName the thread that performed the I/O
public record SocketIOEvent(
        String eventType,
        String host,
        long port,
        long bytes,
        long timeout,
        double durationMillis,
        long timestamp,
        String threadName) {
}
