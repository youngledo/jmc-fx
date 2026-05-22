package com.youngledo.jmcfx.domain.model;

/// Immutable data carrier for one row in the socket I/O histogram.
///
/// @param key           the grouping key (host, port, or host:port)
/// @param host          the remote host address, may be null
/// @param port          the remote port number, may be -1 if not applicable
/// @param readCount     number of read operations
/// @param writeCount    number of write operations
/// @param readSize      total bytes read
/// @param writeSize     total bytes written
/// @param totalDuration total duration across all operations in milliseconds
/// @param maxDuration   maximum single-operation duration in milliseconds
/// @param avgDuration   average duration in milliseconds
public record SocketIOHistogram(
        String key,
        String host,
        long port,
        long readCount,
        long writeCount,
        long readSize,
        long writeSize,
        long totalDuration,
        long maxDuration,
        double avgDuration) {
}
