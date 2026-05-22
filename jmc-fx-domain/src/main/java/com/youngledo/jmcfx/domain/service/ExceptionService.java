package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

/// Port for exception histogram and timeline analysis on a flight recording.
public interface ExceptionService {

	/// Loads the exception histogram grouped by the specified strategy.
	List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping);

	/// Loads the exception timeline chart definition.
	ChartDefinition loadTimeline(RecordingSummary recording);
}
