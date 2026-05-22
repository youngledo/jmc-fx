package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadSummary;

/// Port for thread activity analysis on a flight recording.
public interface ThreadService {

	/// Loads thread activity summaries from the given recording.
	///
	/// @param recording the flight recording to analyze
	/// @return list of thread summaries ordered by sample count descending
	List<ThreadSummary> loadThreadSummaries(RecordingSummary recording);
}
