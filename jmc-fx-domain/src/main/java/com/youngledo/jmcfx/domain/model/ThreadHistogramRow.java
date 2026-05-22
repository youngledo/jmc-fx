package com.youngledo.jmcfx.domain.model;

/// Immutable data carrier for one row in the thread histogram on the Java Application Overview page.
///
/// Each row represents a single thread with its aggregated profiling, IO, blocking,
/// allocation, and exception metrics collected from the recording.
///
/// @param threadName           the thread name
/// @param profilingCount       number of execution samples (CPU profiling hits)
/// @param ioDurationMillis     total time spent in socket + file IO (milliseconds)
/// @param blockedDurationMillis total time spent in blocked state (milliseconds)
/// @param allocatedBytes       total allocated bytes (heap allocation)
/// @param exceptionCount       number of exceptions thrown by this thread
public record ThreadHistogramRow(
        String threadName,
        int profilingCount,
        long ioDurationMillis,
        long blockedDurationMillis,
        long allocatedBytes,
        int exceptionCount) {
}
