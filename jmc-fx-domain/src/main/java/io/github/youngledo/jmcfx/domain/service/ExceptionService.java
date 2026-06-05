package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

/// Port for exception histogram and timeline analysis on a flight recording.
public interface ExceptionService {

	/// Loads the exception histogram grouped by the specified strategy.
	List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping);

	/// Loads the exception timeline chart definition.
	ChartDefinition loadTimeline(RecordingSummary recording);
}
