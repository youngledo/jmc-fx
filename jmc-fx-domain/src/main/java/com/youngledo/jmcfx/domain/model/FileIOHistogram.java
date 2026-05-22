package com.youngledo.jmcfx.domain.model;

/// Immutable data carrier for one row in the file I/O histogram.
///
/// Aggregated by file path. Contains counts, sizes, and timing statistics
/// for all jdk.FileRead and jdk.FileWrite events targeting the same path.
///
/// @param path          the file path, never null
/// @param readCount     number of read operations
/// @param writeCount    number of write operations
/// @param readSize      total bytes read
/// @param writeSize     total bytes written
/// @param totalDuration total duration across all operations in milliseconds
/// @param maxDuration   maximum single-operation duration in milliseconds
/// @param avgDuration   average duration in milliseconds (0 if no operations)
public record FileIOHistogram(
        String path,
        long readCount,
        long writeCount,
        long readSize,
        long writeSize,
        long totalDuration,
        long maxDuration,
        double avgDuration) {
}
