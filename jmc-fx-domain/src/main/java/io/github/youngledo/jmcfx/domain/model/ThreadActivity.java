package io.github.youngledo.jmcfx.domain.model;

/// Immutable data carrier for a single activity interval on a thread timeline lane.
///
/// @param laneType         the category of activity
/// @param startEpochMillis start time as epoch milliseconds
/// @param endEpochMillis   end time as epoch milliseconds
/// @param detail           optional human-readable detail text
public record ThreadActivity(
		ThreadLaneType laneType,
		long startEpochMillis,
		long endEpochMillis,
		String detail) {
}
