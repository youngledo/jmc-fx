package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface ExceptionService {
    List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping);
    ChartDefinition loadTimeline(RecordingSummary recording);
}
