package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

/// Immutable data carrier for a thread's activity summary within a recording.
///
/// @param threadName           the thread name
/// @param threadId             the OS or JVM thread ID
/// @param threadGroup          the thread group name, or empty string if unavailable
/// @param virtual              whether the thread is a virtual thread
/// @param sampleCount          number of execution samples captured for this thread
/// @param blockedDurationMillis total time spent in blocked state (milliseconds)
/// @param activities           time-ordered activity intervals for this thread
public record ThreadSummary(
		String threadName,
		long threadId,
		String threadGroup,
		boolean virtual,
		int sampleCount,
		long blockedDurationMillis,
		List<ThreadActivity> activities) {
}
