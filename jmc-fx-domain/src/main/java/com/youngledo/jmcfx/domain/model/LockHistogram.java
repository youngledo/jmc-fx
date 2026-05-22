package com.youngledo.jmcfx.domain.model;

/// Immutable data carrier for one row in the lock instance histogram.
///
/// @param key             the grouping key (class name, address, or thread name)
/// @param count           number of monitor enter events
/// @param totalDuration   total blocked duration in milliseconds
/// @param maxDuration     maximum single-event blocked duration in milliseconds
/// @param avgDuration     average blocked duration in milliseconds
/// @param inflateCount    number of monitor inflate events for this key, or 0
/// @param distinctThreads number of distinct threads that entered this monitor
/// @param distinctAddresses number of distinct monitor addresses in this group
public record LockHistogram(
        String key,
        long count,
        long totalDuration,
        long maxDuration,
        double avgDuration,
        long inflateCount,
        long distinctThreads,
        long distinctAddresses) {
}
